package backend.prediction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryTileBuffer;
import backend.prediction.directional.MarkovDirectionalModel;
import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.Tile;
import backend.util.TileKey;

public class BasicModel {
	protected int len;
	protected TileHistoryQueue history = null;
	protected ParamsMap paramsMap; // for checking if predictions are actual tiles
	protected MemoryTileBuffer membuf;
	protected DiskTileBuffer diskbuf;
	protected ScidbTileInterface scidbapi;
	public static final double defaultprob = .00000000001; // default assigned confidence value
	public static final int defaultlen = 4; // default history length
	

	public BasicModel(TileHistoryQueue ref, MemoryTileBuffer membuf, DiskTileBuffer diskbuf,ScidbTileInterface api, int len) {
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
	
	// gets ordering of directions by confidence and returns topk viable options
	public List<TileKey> predictTiles(int topk) throws Exception {
		List<TileKey> myresult = new ArrayList<TileKey>();

		// do we have access to the last request?
		List<UserRequest> htrace = history.getHistoryTrace(len);
		if(htrace.size() == 0) {
			return myresult;
		}
		List<DirectionPrediction> order = predictOrder(htrace);
		UserRequest last = htrace.get(htrace.size()-1);
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
		}
		myresult = myresult.subList(0, topk);
		return myresult;
	}
	
	public List<DirectionPrediction> predictOrder(List<UserRequest> htrace) throws Exception {
		return predictOrder(htrace,false);
	}
	
	public List<DirectionPrediction> predictOrder(List<UserRequest> htrace, boolean reverse) throws Exception {
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
		//long end = System.currentTimeMillis();
		/*
		for(DirectionPrediction dp : order) {
			System.out.println(dp);
		}*/
		//System.out.println("time to predict order: "+(end-start)+"ms");
		return order;
	}
	
	public double computeConfidence(Direction d, List<UserRequest> htrace) {
		return defaultprob;
	}
	
	public Tile getTile(TileKey key) {
		Tile t = membuf.getTile(key);
		if(t == null) {
			t = diskbuf.getTile(key);
			if(t == null) {
				t = this.scidbapi.getTile(key);
			}
		}
		return t;
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
		Params p = map1.get(key.zoom);
		if(p == null) {
			return null;
		}
		//System.out.println("recommendation: "+key);
		return key;
	}
	
	public String buildDirectionString(List<UserRequest> trace) {
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
	
	public static TileKey getKeyFromRequest(UserRequest request) {
		List<Integer> id = UtilityFunctions.parseTileIdInteger(request.tile_id);
		return new TileKey(id,request.zoom);
	}
}
