package backend.prediction.directional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.TileKey;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;

public class RandomDirectionalModel {
	private TileHistoryQueue history = null;
	private ParamsMap paramsMap; // for checking if predictions are actual tiles
	private Random generator;
	public static final int seed = 7;
	//public static final int seed = 425752111;

	public RandomDirectionalModel(TileHistoryQueue ref) {
		this.history = ref; // reference to (syncrhonized) global history object
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		this.generator = new Random(seed); // use seed for consistency
	}
	
	// gets ordering of directions by confidence and returns topk viable options
	public List<TileKey> predictTiles(int topk) {
		List<TileKey> myresult = new ArrayList<TileKey>();
		if(this.history == null) {
			return myresult;
		}
		// get prefix of max length
		List<UserRequest> htrace = history.getHistoryTrace(1);
		if(htrace.size() == 0) {
			return myresult;
		}
		List<DirectionPrediction> order = predictOrder();
		UserRequest last = htrace.get(0);
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
	
	// computes an ordering of all directions using randomized confidence values
	public List<DirectionPrediction> predictOrder() {
		List<DirectionPrediction> order = new ArrayList<DirectionPrediction>();
		long start = System.currentTimeMillis();
		// for each direction, compute confidence
		for(Direction d : Direction.values()) {
			DirectionPrediction dp = new DirectionPrediction();
			dp.d = d;
			dp.confidence = generator.nextDouble();
			order.add(dp);
		}
		//Collections.sort(order,Collections.reverseOrder());
		Collections.shuffle(order,generator);
		long end = System.currentTimeMillis();
		/*
		for(DirectionPrediction dp : order) {
			System.out.println(dp);
		}*/
		//System.out.println("time to predict order: "+(end-start)+"ms");
		return order;
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
}

