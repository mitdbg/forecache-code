package backend.prediction.directional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.TileKey;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;

public class MarkovChainDirectionalModel {
	public static final int defaultlen = 4;
	protected  int len;
	protected Map<String,MDMNode> condprobs;
	protected TileHistoryQueue history = null;
	protected ParamsMap paramsMap; // for checking if predictions are actual tiles
	public static final double zeroprob = .00000000001;
	public static final double defaultprob = 1.0/9;
	
	public MarkovChainDirectionalModel() {
		this.len = defaultlen;
		condprobs = new HashMap<String,MDMNode>();
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}

	public MarkovChainDirectionalModel(int len) {
		this.len = len;
		condprobs = new HashMap<String,MDMNode>();
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}
	
	public MarkovChainDirectionalModel(int len, TileHistoryQueue ref) {
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

	
	public double computeConfidence(Direction d, List<UserRequest> trace) {
		String prefix = buildDirectionString(trace);
		MDMNode node = condprobs.get(prefix);
		if(node != null) {
			Double prob = node.probability.get(d); // get counts for this direction
			int count = node.count;
			if(prob != null && prob > 0 && count > 0) { // exact match
				return prob / count;
			} else if (count == 0) { // no observations for this prefix
				return defaultprob;
			} else {
				return zeroprob; // this prefix was never seen
			}
		} else { // no observations for this prefix
			return defaultprob;
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
	
	public boolean isNeighbor(UserRequest prev, UserRequest next) {
		List<Integer> prev_id = UtilityFunctions.parseTileIdInteger(prev.tile_id);
		List<Integer> next_id = UtilityFunctions.parseTileIdInteger(next.tile_id);
		int prev_zoom = prev.zoom;
		int next_zoom = next.zoom;
		
		if(prev_zoom < next_zoom) { // need to zoom in
			int x = next_id.get(0);
			int y = next_id.get(1);
			next_zoom -= 1;
			x /= 2;
			y /= 2;
			return next_zoom == prev_zoom && prev_id.get(0) == x && prev_id.get(1) == y;
		} else if (prev_zoom < next_zoom) { // need to zoom out
			int x = prev_id.get(0);
			int y = prev_id.get(1);
			prev_zoom -= 1;
			x /= 2;
			y /= 2;
			return next_zoom == prev_zoom && next_id.get(0) == x && next_id.get(1) == y;
			
		} else { // on same zoom level
			int x = next_id.get(0) - prev_id.get(0);
			int y = next_id.get(1) - prev_id.get(1);
			if(x < 1) x *= -1;
			if(y < 1) y *= -1;
			return x == 1 && y == 0 || x == 0 && y == 1;
		}
	}
	
	// zoom out until we're at the same level
	public List<TileKey> buildZoomOut(UserRequest prev, UserRequest next) {
		List<TileKey> path = new ArrayList<TileKey>();
		List<Integer> prev_id = UtilityFunctions.parseTileIdInteger(prev.tile_id);
		int p= prev.zoom;
		int n = next.zoom;
		int x = prev_id.get(0);
		int y = prev_id.get(1);
		if(p != n) {
			path.add(new TileKey(prev_id,p));
		}
		while(p > n) {
			p -= 1;
			x /= 2;
			y /= 2;
			List<Integer> tile_id = new ArrayList<Integer>(2);
			tile_id.set(0,x);
			tile_id.set(1,y);
			path.add(new TileKey(tile_id, p));
		}
		return path;
	}
	
	public List<TileKey> buildPan(TileKey prev, UserRequest next) {
		UserRequest p = new UserRequest(prev.buildTileString(),prev.getZoom());
		return buildPan(p,next);
	}
	
	// zoom out until we're at the same level
		public List<TileKey> buildPan(UserRequest prev, UserRequest next) {
			List<TileKey> path = new ArrayList<TileKey>();
			List<Integer> prev_id = UtilityFunctions.parseTileIdInteger(prev.tile_id);
			List<Integer> next_id = UtilityFunctions.parseTileIdInteger(next.tile_id);
			int p= prev.zoom;
			int x = prev_id.get(0);
			int y = prev_id.get(1);
			int x2 = next_id.get(0);
			int y2 = next_id.get(1);
			if(x != x2 && y != y2) {
				path.add(new TileKey(prev_id,p));
			}
			while(x != x2 || y != y2) {
				if(x < x2) {
					x +=1;
				} else if (x > x2) {
					x -= 1;
				} else if (y > y2) {
					y -= 1;
				} else {
					y += 1;
				}
				List<Integer> tile_id = new ArrayList<Integer>(2);
				tile_id.set(0,x);
				tile_id.set(1,y);
				path.add(new TileKey(tile_id, p));
			}
			return path;
		}
	
	
	public List<TileKey> buildPath(UserRequest prev, UserRequest next) {
		List<TileKey> path = new ArrayList<TileKey>();

		int prev_zoom = prev.zoom;
		int next_zoom = next.zoom;
		
		if(prev_zoom > next_zoom) { // need to zoom out
			path.addAll(buildZoomOut(prev,next));
			path.addAll(buildPan(path.get(path.size()-1),next));
		} else if (prev_zoom < next_zoom) { // need to zoom in => zoom out in reverse
			path.addAll(buildZoomOut(next,prev));
			TileKey np = path.remove(path.size()-1);
			path.addAll(buildPan(np,prev));
			Collections.reverse(path);
		} else { // on same zoom level
			if(!prev.tile_id.equals(next.tile_id)) {
				path.addAll(buildPan(prev,next));
			}
		}
		return path;
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
			if(d == null) {
				dirstring = "";
			}
			
			if(dirstring.length() > 0) { // if we have something to check
				this.updateCondProbs(dirstring,d);
			}
			
			if(d != null) {
				dirstring += d;
			}
			if(dirstring.length() > this.len) { // shift characters over by 1
				dirstring = dirstring.substring(1);
			}
			//System.out.println("dirstring: " +dirstring);

			i++;
		}
		/*
		for(String key : frequencies.keySet()) {
			System.out.println("key: "+key+", count: "+frequencies.get(key));
		}*/
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
}
