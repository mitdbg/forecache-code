package backend.prediction.directional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.io.ArpaLmReader;
import edu.berkeley.nlp.lm.map.NgramMap;
import edu.berkeley.nlp.lm.values.ProbBackoffPair;

import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.TileKey;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;

public class NGramDirectionalModel {
	public static final int defaultlen = 4;
	protected  int len;
	protected Map<String,MDMNode> condprobs;
	protected TileHistoryQueue history = null;
	protected ParamsMap paramsMap; // for checking if predictions are actual tiles
	public static final double defaultprob = .00000000001;
	
	public NGramDirectionalModel() {
		this.len = defaultlen;
		condprobs = new HashMap<String,MDMNode>();
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}

	public NGramDirectionalModel(int len) {
		this.len = len;
		condprobs = new HashMap<String,MDMNode>();
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}
	
	public NGramDirectionalModel(int len, TileHistoryQueue ref) {
		this.len = len;
		condprobs = new HashMap<String,MDMNode>();
		this.history = ref; // reference to (syncrhonized) global history object
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}
	
	public int getMaxLen() {
		return this.len;
	}
	
	// gets ordering of directions by confidence and returns topk viable options
	public List<TileKey> predictTiles(int topk) {
		List<TileKey> myresult = new ArrayList<TileKey>();
		if(this.history == null) {
			return myresult;
		}
		// get prefix of max length
		List<UserRequest> htrace = history.getHistoryTrace(this.len);
		if(htrace.size() == 0) {
			return myresult;
		}
		List<DirectionPrediction> order = predictOrder(htrace);
		int end = this.len - 1;
		if(end >= htrace.size()) {
			end = htrace.size() - 1;
		}
		UserRequest last = htrace.get(end);
		for(DirectionPrediction dp : order) {
			TileKey val = this.directionToTile(last, dp.d);
			if(val != null) {
				myresult.add(val);
			}
		}
		if(topk >= myresult.size()) { // truncate if list is too long
			topk = myresult.size() - 1;
		}
		myresult = myresult.subList(0, topk);
		return myresult;
	}
	
	public static void testing() {
		List<String> sentences = new ArrayList<String>();
		sentences.add("testing this out");
		sentences.add("testing sentence building");
		sentences.add("this sentence building testing");
		sentences.add("this sentence is testing");
		sentences.add("this sentence building");
		sentences.add("sentence testing");
		sentences.add("this building testing sentence");
		StringWordIndexer wordIndexer = new StringWordIndexer();
		wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
		wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
		wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);
		ArrayEncodedProbBackoffLm<String> lm = LmReadersExtension.readKneserNeyLmFromStrings(sentences, wordIndexer, 3); // trigrams
		
		int[] bigram = {wordIndexer.getIndexPossiblyUnk("testing"),wordIndexer.getIndexPossiblyUnk("this")};
		System.out.print("indices of: [testing,this]:");
		System.out.print(" "+bigram[0]+", ");
		System.out.println(" "+bigram[1]);
		NgramMap<ProbBackoffPair> x = lm.getNgramMap();
		ProbBackoffPair pbp = x.get(bigram, 0, 2); // end index must be 1 past the index you want to include
		
		// this gives me the very basic Markov stuff
		if(pbp != null) {
			System.out.println("probability of bigram: "+pbp.prob);
		} else {
			System.out.println("bigram doesn't exist!");
		}
		
		// this is what we want!!!! Does the smart ngram stuff
		System.out.println("sentence log probability: "+lm.getLogProb(bigram,0,2));
		
		// the probabilities are stored as log(prob), and are thus negative
		int[] trigram = {bigram[0],bigram[0],bigram[0]};
		System.out.println("trigram sentence log probability: "+lm.getLogProb(trigram,0,3));
	}
	
	// computes an ordering of all directions using confidence values
	public List<DirectionPrediction> predictOrder(List<UserRequest> htrace) {
		List<DirectionPrediction> order = new ArrayList<DirectionPrediction>();
		long start = System.currentTimeMillis();
		// for each direction, compute confidence
		for(Direction d : Direction.values()) {
			DirectionPrediction dp = new DirectionPrediction();
			dp.d = d;
			dp.confidence = computeConfidence(d,htrace);
			if(dp.confidence < defaultprob) {
				dp.confidence = defaultprob;
			}
			order.add(dp);
		}
		Collections.sort(order,Collections.reverseOrder());
		long end = System.currentTimeMillis();
		/*
		for(DirectionPrediction dp : order) {
			System.out.println(dp);
		}
		*/
		//System.out.println("time to predict order: "+(end-start)+"ms");
		return order;
	}
	
	public static void createKneserNeyLmFromStrings(List<String> sentences, final int lmOrder) {
		
	}
	
	public double computeBaseProb(Direction d) {
		MDMNode node = condprobs.get("");
		if(node != null) {
			Double prob = node.probability.get(d);
			if(prob != null) { // exact match
				return prob;
			} else {
				return defaultprob;
			}
		} else {
			return defaultprob;
		}
	}
	
	public double computeConfidence(Direction d, List<UserRequest> trace) {
		if(trace.size() <= 1) { // if there is no prior direction
			return computeBaseProb(d);
		}
		// trace is at least 2 here
		int last = trace.size() - 1;
		int nextlast = last - 1;
		String prefix = buildDirectionString(trace);
		double prior = 1.0;
		MDMNode node = condprobs.get(prefix);
		if(node != null) {
			Double prob = node.probability.get(d);
			if(prob != null) { // exact match
				return prob;
			} else { // find longest relevant prefix and extrapolate
				// get the last direction taken
				Direction subd = UtilityFunctions.getDirection(trace.get(nextlast),trace.get(last));
				prior = computeConfidence(subd,trace.subList(1, trace.size()));
				return computeBaseProb(d) * prior;
			}
			
		} else { // find longest relevant prefix and extrapolate
			// get the last direction taken
			Direction subd = UtilityFunctions.getDirection(trace.get(nextlast),trace.get(last));
			prior = computeConfidence(subd,trace.subList(1, trace.size()));
			return computeBaseProb(d) * prior;
		}
	}
	
	public void predictTest() {
		if(this.history == null) {
			return;
		}
		long start = System.currentTimeMillis();
		List<UserRequest> htrace = history.getHistoryTrace(this.len);
		String dirstring = buildDirectionString(htrace);
		TileKey prediction = null;
		System.out.println("dirstring: "+dirstring);
		for(int i = 0; i < dirstring.length(); i++) {
			String sub = dirstring.substring(i);
			MDMNode node = condprobs.get(sub);
			if(node != null) {
				Direction d = null;
				double mprob = 0;
				for(Direction dkey : node.probability.keySet()) {
					double cprob = node.probability.get(dkey);
					if((d == null) || (cprob > mprob)) {
						d = dkey;
						mprob = cprob;
					}
				}
				System.out.println("found hit at prefix: '"+sub+"'");
				System.out.println("'"+d+"' is the most likely next direction: "+mprob);
				int end = this.len - 1;
				if(end >= htrace.size()) {
					end = htrace.size() - 1;
				}
				prediction = directionToTile(htrace.get(end),d);
				if(prediction != null) {
					break;
				}
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("time to predict: "+(end-start)+"ms");
	}
	
	public TileKey directionToTile(UserRequest prev, Direction d) {
		List<Integer> tile_id = UtilityFunctions.parseTileIdInteger(prev.tile_id);
		int x = tile_id.get(0);
		int y = tile_id.get(1);
		int zoom = prev.zoom;
		
		// if zooming in, update values
		if((d == Direction.IN1) || (d == Direction.IN2) || (d == Direction.IN3) || (d == Direction.IN0)) {
			zoom++;
			x *= 2;
			y *=2;
			tile_id.set(0,x);
			tile_id.set(1,y);
		}
		
		switch(d) {
		case UP:
			tile_id.set(1,y+1);
			break;
		case DOWN:
			tile_id.set(1,y-1);
			break;
		case LEFT:
			tile_id.set(0,x-1);
			break;
		case RIGHT:
			tile_id.set(0,x+1);
			break;
		case OUT:
			zoom -= 1;
			x /= 2;
			y /= 2;
			tile_id.set(0,x);
			tile_id.set(1,y);
			break;
		case IN0: // handled above
			break;
		case IN1:
			tile_id.set(1,y+1);
			break;
		case IN3:
			tile_id.set(0,x+1);
			break;
		case IN2:
			tile_id.set(0,x+1);
			tile_id.set(1,y+1);
			break;
		}
		TileKey key = new TileKey(tile_id,zoom);
		//System.out.println("last access: ("+prev.tile_id+", "+prev.zoom+")");
		Map<Integer,Params> map1 = this.paramsMap.get(key.buildTileString());
		if(map1 == null) {
			return null;
		}
		Params p = map1.get(key.getZoom());
		if(p == null) {
			return null;
		}
		//System.out.println("recommendation: "+key);
		return key;
	}
	
	public static String buildDirectionString(List<UserRequest> trace) {
		if(trace.size() < 2) {
			return "";
		}
		String dirstring = "";
		int i = 1;
		UserRequest n = trace.get(0);
		while(i < trace.size()) {
			UserRequest p = n;
			n = trace.get(i);
			Direction d = UtilityFunctions.getDirection(p,n);
			if(d != null) {
				dirstring += d;
			}
			i++;
		}
		return dirstring;
	}
	
	protected void updateCondProbs(String prefix, Direction d) {
		MDMNode node = condprobs.get(prefix);
		if(node == null) {
			node = new MDMNode();
			condprobs.put(prefix,node);
		}
		node.count++;
		Double prob = node.probability.get(d);
		if(prob == null) {
			node.probability.put(d, 1.0);
		} else {
			node.probability.put(d, prob+1);
		}
	}
	
	public void train(List<UserRequest> trace) {
		if(trace.size() < 2) {
			return;
		}
		String dirstring = "";
		int i = 1;
		UserRequest n = trace.get(0);
		//System.out.println("n:"+n.tile_id+","+n.zoom);
		while(i < trace.size()) {
			UserRequest p = n;
			n = trace.get(i);
			Direction d = UtilityFunctions.getDirection(p,n);
			if(d != null) {
				dirstring += d;
			} else {
				dirstring = ""; // reset, user went back to [0,0],0
			}
			if(dirstring.length() > this.len) { // head of string changed
				dirstring = dirstring.substring(1);
			}
			if(dirstring.length() > 0) {
				for(int j = dirstring.length()-1; j >= 0; j--) {
					String sub = dirstring.substring(j);
					//System.out.println("sub: " +sub);
					if(sub.length() > 1) {
						String prefix = sub.substring(0,sub.length()-1);
						//System.out.println(prefix+"-"+d);
						this.updateCondProbs(prefix,d);
						
					} else { // single char
						this.updateCondProbs("",d);
					}
				}
			}
			//System.out.println("dirstring: " +dirstring);

			i++;
		}/*
		for(String key : frequencies.keySet()) {
			System.out.println("key: "+key+", count: "+frequencies.get(key));
		}*/
	}
	
	// call this last, after all train calls have happened
	public void finishTraining() {
		learnProbabilities();
	}
	
	// call this last, after all train calls have happened
	public void learnProbabilities() {
		//make the actual probabilities
		for(String key : condprobs.keySet()) {
			//System.out.println("prefix: "+key);
			MDMNode node = condprobs.get(key);
			for(Direction dkey : node.probability.keySet()) {
				Double prob = node.probability.get(dkey);
				node.probability.put(dkey, prob/node.count);
				//System.out.println("conditional probability for "+dkey+": "+(prob/node.count));
			}
		}
	}
	
	public static TileKey getKeyFromRequest(UserRequest request) {
		List<Integer> id = UtilityFunctions.parseTileIdInteger(request.tile_id);
		return new TileKey(id,request.zoom);
	}
	
	protected class MDMNode {
		public int count = 0;
		public Map<Direction,Double> probability;
		
		MDMNode() {
			this.probability = new HashMap<Direction,Double>();
		}
	}
	
	public static void main(String[] args) {
		testing();
	}
}
