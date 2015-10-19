package abstraction.prediction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import abstraction.enums.Direction;
import abstraction.enums.Model;
import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.structures.SessionMetadata;
import abstraction.structures.UserRequest;
import abstraction.util.UtilityFunctions;

public class BasicModel {
	protected int len;
	protected boolean useDistanceCorrection = true;
	public static final double defaultprob = .00000000001; // default assigned confidence value
	public List<NewTileKey> roi = null;
	protected Model m = null;
	
	
	public BasicModel(int len) {
		this.len = len;
	}
	
	public void computeSignaturesInParallel(SessionMetadata md, DefinedTileView dtv, List<NewTileKey> ids) {}
	
	public List<NewTileKey> orderCandidates(SessionMetadata md, DefinedTileView dtv,
			List<NewTileKey> candidates) {
		//long a = System.currentTimeMillis();
		updateRoi(md,dtv);
		//long b = System.currentTimeMillis();
		List<NewTileKey> htrace = md.history.getHistoryTrace(len);
		if(htrace.size() == 0) {
			return new ArrayList<NewTileKey>();
		}
		NewTileKey prev = htrace.get(htrace.size() - 1);
		List<NewTileKey> myresult = new ArrayList<NewTileKey>();
		List<TilePrediction> order = new ArrayList<TilePrediction>();
		computeSignaturesInParallel(md,dtv,candidates);
		// for each direction, compute confidence
		for(NewTileKey key : candidates) {
			TilePrediction tp = new TilePrediction(this.m);
			tp.id = key;
			tp.confidence = computeConfidence(md,dtv,key, htrace);
			tp.distance = computeDistance(md,dtv,key,htrace);
			tp.useDistance = useDistanceCorrection;
			if(tp.useDistance) {
				tp.physicalDistance = UtilityFunctions.manhattanDist(prev, key);
			}
			order.add(tp);
		}
		//long c = System.currentTimeMillis();
		Collections.sort(order);
		for(int i = 0; i < order.size(); i++) {
			TilePrediction tp = order.get(i);
			myresult.add(tp.id);
			//System.out.print(tp+" "+tp.physicalDistance+" ");
			//System.out.println(tp.id);
		}
		//System.out.println();
		//long d = System.currentTimeMillis();
		//System.out.println("roi:sort-"+(d-c)+",predict-"+(c-b)+",roi-"+(b-a));
		return myresult;
	}
	
	public void updateRoi(SessionMetadata md, DefinedTileView dtv) {
		roi = md.history.getLastRoi();
	}
	
	//TODO: override these to do ROI predictions
	public Double computeConfidence(SessionMetadata md, DefinedTileView dtv, NewTileKey id, List<NewTileKey> htrace) {
		return defaultprob;
	}
	
	public Double computeDistance(SessionMetadata md, DefinedTileView dtv, NewTileKey id, List<NewTileKey> htrace) {
		return null;
	}
	
	// gets ordering of directions by confidence and returns topk viable options
	public List<NewTileKey> predictTiles(SessionMetadata md, DefinedTileView dtv, int topk) {
		List<NewTileKey> myresult = new ArrayList<NewTileKey>();

		// do we have access to the last request?
		List<NewTileKey> htrace = md.history.getHistoryTrace(len);
		if(htrace.size() == 0) {
			return myresult;
		}
		List<DirectionPrediction> order = predictOrder(md,dtv,htrace);
		NewTileKey last = htrace.get(htrace.size()-1);
		for(int i = 0; i < order.size(); i++) {
			DirectionPrediction dp = order.get(i);
			NewTileKey val = this.DirectionToTile(dtv, last, dp.d);
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
	
	public List<DirectionPrediction> predictOrder(SessionMetadata md, DefinedTileView dtv, List<NewTileKey> htrace) {
		return predictOrder(md,dtv,htrace,false);
	}
	
	public List<DirectionPrediction> predictOrder(SessionMetadata md, DefinedTileView dtv, List<NewTileKey> htrace, boolean reverse) {
		List<DirectionPrediction> order = new ArrayList<DirectionPrediction>();
		//long start = System.currentTimeMillis();
		// for each direction, compute confidence
		for(Direction d : Direction.values()) {
			DirectionPrediction dp = new DirectionPrediction();
			
			dp.d = d;
			dp.confidence = computeConfidence(md,dtv,d, htrace);
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
	
	public double computeConfidence(SessionMetadata md, DefinedTileView dtv,
			Direction d, List<NewTileKey> htrace) {
		return defaultprob;
	}
	
	public NewTileKey DirectionToTile(DefinedTileView dtv, NewTileKey prev, Direction d) {
		int[] tile_id = prev.dimIndices.clone();
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
		NewTileKey key = new NewTileKey(tile_id,zoom);
		//System.out.println("last access: ("+prev.tile_id+", "+prev.zoom+")");
		if(!dtv.containsKey(key)) {
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
	
	public String buildDirectionStringFromKey(List<NewTileKey> trace) {
		if(trace.size() < 2) {
			return "";
		}
		String dirstring = "";
		int i = 1;
		NewTileKey n = trace.get(0);
		while(i < trace.size()) {
			NewTileKey p = n;
			n = trace.get(i);
			Direction d = UtilityFunctions.getDirection(p,n);
			if(d != null) {
				dirstring += d;
			}
			i++;
		}
		return dirstring;
	}
	
	public static NewTileKey getKeyFromRequest(UserRequest request) {
		int[] id = UtilityFunctions.parseTileIdInteger(request.tile_id);
		return new NewTileKey(id,request.zoom);
	}
	
	public List<NewTileKey> getCandidates(SessionMetadata md,
			DefinedTileView dtv, double maxDist) {
		NewTileKey last = md.history.getLast();
		if(last == null) return new ArrayList<NewTileKey>();
		return getCandidates(dtv,last,maxDist);
	}
	
	public List<NewTileKey> getCandidates(DefinedTileView dtv,
			NewTileKey current, double maxDist) {
		return dtv.getCandidateTileKeys(current, maxDist);
	}
}
