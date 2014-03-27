package backend.prefetch.similarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.prefetch.TileHistoryQueue;
import backend.util.Direction;
import backend.util.TileKey;

import utils.UserRequest;
import utils.UtilityFunctions;

public class MarkovDirectionalModel {
	public static final int defaultlen = 2;
	private  int len;
	private Map<String,Integer> frequencies;
	private Map<String,MDMNode> condprobs;
	private TileHistoryQueue history = null;

	public MarkovDirectionalModel(int len) {
		this.len = len;
		frequencies = new HashMap<String,Integer>();
		condprobs = new HashMap<String,MDMNode>();
	}
	
	public MarkovDirectionalModel(int len, TileHistoryQueue ref) {
		this.len = len;
		frequencies = new HashMap<String,Integer>();
		condprobs = new HashMap<String,MDMNode>();
		this.history = ref; // reference to (syncrhonized) global history object
	}
	
	public int getMaxLen() {
		return this.len;
	}
	
	public void predict() {
		if(this.history == null) {
			return;
		}
		List<UserRequest> htrace = history.getHistoryTrace(this.len);
		String dirstring = buildDirectionString(htrace);
		if(dirstring.length() == 0) {
			return;
		}
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
				break;
			}
		}
	}
	
	public String buildDirectionString(List<UserRequest> trace) {
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
						System.out.println(prefix+"-"+d);
						this.updateCondProbs(prefix,d);
						
					} else { // single char
						this.updateCondProbs("",d);
					}
					/*
					Integer count = frequencies.get(sub);
					if(count == null) {
						frequencies.put(sub, 1);
					} else {
						frequencies.put(sub,count+1);
					}
					*/
				}
			}
			//System.out.println("dirstring: " +dirstring);

			i++;
		}/*
		for(String key : frequencies.keySet()) {
			System.out.println("key: "+key+", count: "+frequencies.get(key));
		}*/
		
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
	
	public Direction getDirection(List<Integer> p_id, List<Integer> n_id, int pzoom, int nzoom) {
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
