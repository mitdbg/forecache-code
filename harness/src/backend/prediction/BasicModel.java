package backend.prediction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;
import backend.disk.DiskNiceTileBuffer;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryTileBuffer;
import backend.prediction.directional.MarkovDirectionalModel;
import backend.util.Direction;
import backend.util.NiceTile;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.Tile;
import backend.util.TileKey;

public class BasicModel {
	protected int len;
	protected TileHistoryQueue history = null;
	protected ParamsMap paramsMap; // for checking if predictions are actual tiles
	protected MemoryNiceTileBuffer membuf;
	protected DiskNiceTileBuffer diskbuf;
	protected ScidbTileInterface scidbapi;
	protected boolean useDistanceCorrection = true;
	public static final double defaultprob = .00000000001; // default assigned confidence value
	public static final int defaultlen = 4; // default history length
	public List<TileKey> roi = null;
	

	public BasicModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len) {
		this.history = ref; // reference to (syncrhonized) global history object
		this.membuf = membuf;
		this.diskbuf = diskbuf;
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		this.scidbapi = api;
		this.len = len;
	}
	
	public int getMaxLen() {
		return this.len;
	}
	
	public List<TileKey> orderCandidates(List<TileKey> candidates) {
		updateRoi();
		List<TileKey> htrace = history.getHistoryTrace(len);
		if(htrace.size() == 0) {
			return new ArrayList<TileKey>();
		}
		TileKey prev = htrace.get(htrace.size() - 1);
		List<TileKey> myresult = new ArrayList<TileKey>();
		List<TilePrediction> order = new ArrayList<TilePrediction>();
		// for each direction, compute confidence
		for(TileKey key : candidates) {
			TilePrediction tp = new TilePrediction();
			tp.id = key;
			tp.confidence = computeConfidence(key, htrace);
			tp.distance = computeDistance(key,htrace);
			tp.useDistance = useDistanceCorrection;
			if(tp.useDistance) {
				tp.physicalDistance = UtilityFunctions.manhattanDist(prev, key);
			}
			order.add(tp);
		}
		Collections.sort(order);
		for(int i = 0; i < order.size(); i++) {
			TilePrediction tp = order.get(i);
			myresult.add(tp.id);
			//System.out.print(tp+" ");
			//System.out.println(id);
		}
		//System.out.println();

		return myresult;
	}
	
	public void updateRoi() {
		roi = history.getLastRoi();
	}
	
	//TODO: override these to do ROI predictions
	public Double computeConfidence(TileKey id, List<TileKey> htrace) {
		return defaultprob;
	}
	
	public Double computeDistance(TileKey id, List<TileKey> htrace) {
		return null;
	}
	
	// gets ordering of directions by confidence and returns topk viable options
	public List<TileKey> predictTiles(int topk) {
		List<TileKey> myresult = new ArrayList<TileKey>();

		// do we have access to the last request?
		List<TileKey> htrace = history.getHistoryTrace(len);
		if(htrace.size() == 0) {
			return myresult;
		}
		List<DirectionPrediction> order = predictOrder(htrace);
		TileKey last = htrace.get(htrace.size()-1);
		for(int i = 0; i < order.size(); i++) {
			DirectionPrediction dp = order.get(i);
			TileKey val = this.DirectionToTile(last, dp.d);
			if(val != null) {
				myresult.add(val);
				//System.out.println(val);
			}
		}
		//System.out.println("viable options: "+myresult.size());
		if(topk >= myresult.size()) { // truncate if list is too long
			topk = myresult.size() - 1;
			if(topk < 0) {
				topk = 0;
			}
		}
		myresult = myresult.subList(0, topk);
		return myresult;
	}
	
	public List<DirectionPrediction> predictOrder(List<TileKey> htrace) {
		return predictOrder(htrace,false);
	}
	
	public List<DirectionPrediction> predictOrder(List<TileKey> htrace, boolean reverse) {
		List<DirectionPrediction> order = new ArrayList<DirectionPrediction>();
		//long start = System.currentTimeMillis();
		// for each direction, compute confidence
		for(Direction d : Direction.values()) {
			DirectionPrediction dp = new DirectionPrediction();
			
			dp.d = d;
			dp.confidence = computeConfidence(d, htrace);
			order.add(dp);
		}
		if(!reverse) {
			Collections.sort(order);
		} else {
			Collections.sort(order,Collections.reverseOrder()); // smaller numbers are better here
		}
		//longpend = System.currentTimeMillis();
		/*
		for(DirectionPrediction dp : order) {
			System.out.println(dp);
		}*/
		//System.out.println("time to predict order: "+(end-start)+"ms");
		return order;
	}
	
	public double computeConfidence(Direction d, List<TileKey> htrace) {
		return defaultprob;
	}
	
	public NiceTile getTile(TileKey key) {
		NiceTile t = membuf.getTile(key);
		if(t == null) {
			t = diskbuf.getTile(key);
			if(t == null) {
				t = this.scidbapi.getNiceTile(key);
			}
		}
		return t;
	}
	
	public TileKey DirectionToTile(TileKey prev, Direction d) {
		int[] tile_id = prev.id.clone();
		int x = tile_id[0];
		int y = tile_id[1];
		int zoom = prev.zoom;
		
		// if zooming in, update values
		if((d == Direction.IN1) || (d == Direction.IN2) || (d == Direction.IN3) || (d == Direction.IN0)) {
			zoom++;
			x *= 2;
			y *=2;
			tile_id[0] = x;
			tile_id[1] = y;
		}
		
		switch(d) {
		case UP:
			tile_id[1] =y+1;
			break;
		case DOWN:
			tile_id[1] =y-1;
			break;
		case LEFT:
			tile_id[0] =x-1;
			break;
		case RIGHT:
			tile_id[0] =x+1;
			break;
		case OUT:
			zoom -= 1;
			x /= 2;
			y /= 2;
			tile_id[0] =x;
			tile_id[1] =y;
			break;
		case IN0: // handled above
			break;
		case IN1:
			tile_id[1] =y+1;
			break;
		case IN3:
			tile_id[0] =x+1;
			break;
		case IN2:
			tile_id[0] =x+1;
			tile_id[1] =y+1;
			break;
		}
		TileKey key = new TileKey(tile_id,zoom);
		//System.out.println("last access: ("+prev.tile_id+", "+prev.zoom+")");
		if(!this.paramsMap.allKeys.containsKey(key)) {
			return null;
		}
		//System.out.println("recommendation: "+key);
		return key;
	}
	
	public String buildDirectionStringFromString(List<UserRequest> trace) {
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
	
	public String buildDirectionStringFromKey(List<TileKey> trace) {
		if(trace.size() < 2) {
			return "";
		}
		String dirstring = "";
		int i = 1;
		TileKey n = trace.get(0);
		while(i < trace.size()) {
			TileKey p = n;
			n = trace.get(i);
			Direction d = UtilityFunctions.getDirection(p,n);
			if(d != null) {
				dirstring += d;
			}
			i++;
		}
		return dirstring;
	}
	
	public static TileKey getKeyFromRequest(UserRequest request) {
		int[] id = UtilityFunctions.parseTileIdInteger(request.tile_id);
		return new TileKey(id,request.zoom);
	}
	
	public List<TileKey> getCandidates(double maxDist) {
		TileKey last = this.history.getLast();
		if(last == null) return new ArrayList<TileKey>();
		return getCandidates(last,maxDist);
	}
	
	public List<TileKey> getCandidates(TileKey current, double maxDist) {
		List<TileKey> candidates = new ArrayList<TileKey>();
		for(TileKey pcand : this.paramsMap.allKeysSet) {
			double dist = UtilityFunctions.manhattanDist(pcand, current);
			if((dist <= maxDist) && (dist > 0)) { // don't include the tile itself
				candidates.add(pcand);
			}
		}
		
		// include everything that is already in the cache
		//candidates.addAll(this.membuf.getAllTileKeys());

		return candidates;
	}
}
