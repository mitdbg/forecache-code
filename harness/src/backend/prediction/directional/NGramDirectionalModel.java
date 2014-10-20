package backend.prediction.directional;

import java.util.ArrayList;
import java.util.Collections;
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
	protected TileHistoryQueue history = null;
	protected ParamsMap paramsMap; // for checking if predictions are actual tiles
	public static final double defaultprob = .00000000001;
	protected static List<String> sentences;
	protected static StringWordIndexer wordIndexer;
	protected static ArrayEncodedProbBackoffLm<String> lm;
	
	protected void setupNgramModel() {
		sentences = new ArrayList<String>();
		wordIndexer = new StringWordIndexer();
		wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
		wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
		wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);
		lm = null;
	}
	
	public NGramDirectionalModel() {
		this.len = defaultlen;
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		setupNgramModel();
	}

	public NGramDirectionalModel(int len) {
		this.len = len;
		sentences = new ArrayList<String>();
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		setupNgramModel();
	}
	
	public NGramDirectionalModel(int len, TileHistoryQueue ref) {
		this.len = len;
		sentences = new ArrayList<String>();
		this.history = ref; // reference to (syncrhonized) global history object
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		setupNgramModel();
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
			dp.confidence = computeConfidence(d,htrace);/*
			if(dp.confidence < defaultprob) {
				dp.confidence = defaultprob;
			}*/
			order.add(dp);
		}
		Collections.sort(order,Collections.reverseOrder());
		long end = System.currentTimeMillis();
		
		for(DirectionPrediction dp : order) {
			System.out.println(dp);
		}
		
		//System.out.println("time to predict order: "+(end-start)+"ms");
		return order;
	}
	
	public double computeConfidence(Direction d, List<UserRequest> trace) {
		String p = buildDirectionString(trace);
		p += " " + d.getWord();
		System.out.println("dirstring: \""+p +"\"");
		String[] prefix = p.split(" ");
		int[] ngram = new int[prefix.length];
		for(int i = 0; i < prefix.length; i++) {
			ngram[i] = wordIndexer.getIndexPossiblyUnk(prefix[i]);
		}

		return lm.getLogProb(ngram,0,prefix.length);
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
		UserRequest p = n;
		n = trace.get(i);
		Direction d = UtilityFunctions.getDirection(p,n);
		if(d != null) {
			dirstring += d.getWord();
		}
		i++;
		
		while(i < trace.size()) {
			p = n;
			n = trace.get(i);
			d = UtilityFunctions.getDirection(p,n);
			if(d != null) {
				dirstring += " " + d.getWord();
			}
			i++;
		}
		return dirstring;
	}
	
	public void finishTraining() {
		System.out.println("len: "+this.len+", sentences: "+sentences.size());
		
		// this library assumes ngrams of order 3 or higher
		lm = LmReadersExtension.readKneserNeyLmFromStrings(sentences, wordIndexer, Math.max(3,this.len));
	}
	
	public void train(List<UserRequest> trace) {
		sentences.add(buildDirectionString(trace));
	}
	
	public static TileKey getKeyFromRequest(UserRequest request) {
		List<Integer> id = UtilityFunctions.parseTileIdInteger(request.tile_id);
		return new TileKey(id,request.zoom);
	}
	
	public static void main(String[] args) {
		testing();
	}
}
