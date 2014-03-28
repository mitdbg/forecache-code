package backend.prefetch.similarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.prefetch.DirectionPrediction;
import backend.prefetch.TileHistoryQueue;
import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.TileKey;

import utils.UserRequest;
import utils.UtilityFunctions;

public class MarkovDirectionalModel {
	public static final int defaultlen = 4;
	private  int len;
	//private Map<String,Integer> frequencies;
	private Map<String,MDMNode> condprobs;
	private TileHistoryQueue history = null;
	private ParamsMap paramsMap; // for checking if predictions are actual tiles
	public static final double defaultprob = .00000000001;

	public MarkovDirectionalModel(int len) {
		this.len = len;
		//frequencies = new HashMap<String,Integer>();
		condprobs = new HashMap<String,MDMNode>();
		this.paramsMap = new ParamsMap(ParamsMap.defaultparamsfile,ParamsMap.defualtdelim);
	}
	
	public MarkovDirectionalModel(int len, TileHistoryQueue ref) {
		this.len = len;
		//frequencies = new HashMap<String,Integer>();
		condprobs = new HashMap<String,MDMNode>();
		this.history = ref; // reference to (syncrhonized) global history object
		this.paramsMap = new ParamsMap(ParamsMap.defaultparamsfile,ParamsMap.defualtdelim);
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
			TileKey val = this.DirectionToTile(last, dp.d);
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
		System.out.println("time to predict order: "+(end-start)+"ms");
		return order;
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
		String prefix = this.buildDirectionString(trace);
		double prior = 1.0;
		MDMNode node = condprobs.get(prefix);
		if(node != null) {
			Double prob = node.probability.get(d);
			if(prob != null) { // exact match
				return prob;
			} else { // find longest relevant prefix and extrapolate
				// get the last direction taken
				Direction subd = getDirection(trace.get(nextlast),trace.get(last));
				prior = computeConfidence(subd,trace.subList(1, trace.size()));
				return computeBaseProb(d) * prior;
			}
			
		} else { // find longest relevant prefix and extrapolate
			// get the last direction taken
			Direction subd = getDirection(trace.get(nextlast),trace.get(last));
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
				prediction = DirectionToTile(htrace.get(end),d);
				if(prediction != null) {
					break;
				}
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("time to predict: "+(end-start)+"ms");
	}
	
	public TileKey DirectionToTile(UserRequest prev, Direction d) {
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
		//System.out.println("n:"+n.tile_id+","+n.zoom);
		List<Integer> n_id = UtilityFunctions.parseTileIdInteger(n.tile_id);
		List<Integer> p_id;
		int pzoom;
		int nzoom = n.zoom;
		while(i < trace.size()) {
			p_id = n_id;
			pzoom = nzoom;
			n = trace.get(i);
			//System.out.println("n:"+n.tile_id+","+n.zoom);
			n_id = UtilityFunctions.parseTileIdInteger(n.tile_id);
			nzoom = n.zoom;
			Direction d = getDirection(p_id,n_id,pzoom,nzoom);
			if(d != null) {
				dirstring += d;
			}
			i++;
		}
		return dirstring;
	}
	
	private void updateCondProbs(String prefix, Direction d) {
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
			Direction d = getDirection(p,n);
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
	public void learnProbabilities() {
		//make the actual probabilities
		for(String key : condprobs.keySet()) {
			System.out.println("prefix: "+key);
			MDMNode node = condprobs.get(key);
			for(Direction dkey : node.probability.keySet()) {
				Double prob = node.probability.get(dkey);
				node.probability.put(dkey, prob/node.count);
				System.out.println("conditional probability for "+dkey+": "+(prob/node.count));
			}
		}
	}
	
	public static Direction getDirection(UserRequest p, UserRequest n) {
		List<Integer> n_id = UtilityFunctions.parseTileIdInteger(n.tile_id);
		List<Integer> p_id = UtilityFunctions.parseTileIdInteger(p.tile_id);
		return getDirection(p_id,n_id,p.zoom,n.zoom);
	}
	
	public static Direction getDirection(List<Integer> p_id, List<Integer> n_id, int pzoom, int nzoom) {
		int zoomdiff = nzoom - pzoom;
		int xdiff = n_id.get(0) - p_id.get(0);
		int ydiff = n_id.get(1) - p_id.get(1);
		//System.out.println("zoomdiff: "+zoomdiff+", xdiff: "+xdiff+", ydiff: "+ydiff);
		if(zoomdiff < -1) { // user reset back to top level tile
			return null;
		} else if(zoomdiff < 0) { // zoom out
			return Direction.OUT;
		} else if(zoomdiff > 0) { // zoom in
			xdiff = n_id.get(0) - 2 * p_id.get(0);
			ydiff = n_id.get(1) - 2 * p_id.get(1);
			if((xdiff > 0) && (ydiff > 0)) {
				return Direction.IN2;
			} else if(xdiff > 0) {
				return Direction.IN3;
			} else if(ydiff > 0) {
				return Direction.IN1;
			} else {
				return Direction.IN0;
			}
		} else if(xdiff > 0) {
			return Direction.RIGHT;
		} else if(xdiff < 0) {
			return Direction.LEFT;
		} else if(ydiff > 0) {
			return Direction.UP;
		} else if(ydiff < 0 ){
			return Direction.DOWN;
		}
		return null;
	}
	
	private class MDMNode {
		public int count = 0;
		public Map<Direction,Double> probability;
		
		MDMNode() {
			this.probability = new HashMap<Direction,Double>();
		}
	}
}
