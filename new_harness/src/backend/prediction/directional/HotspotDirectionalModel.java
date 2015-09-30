package backend.prediction.directional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import abstraction.prediction.DefinedTileView;
import abstraction.prediction.SessionMetadata;
import backend.prediction.DirectionPrediction;
import abstraction.util.Direction;
import abstraction.util.Model;
import abstraction.util.NewTileKey;
import abstraction.util.UtilityFunctions;

import abstraction.util.UserRequest;

public class HotspotDirectionalModel extends MomentumDirectionalModel {
	protected Map<NewTileKey,Integer> hotspots;
	public static final int defaulthotspotlen = 5;
	public static final double maxdistance = 2.0;
	protected int hotspotlen;
	
	public HotspotDirectionalModel(int len) {
		super(len);
		this.hotspots = new HashMap<NewTileKey,Integer>();
		this.hotspotlen = defaulthotspotlen;
		this.m = Model.HOTSPOT;
	}
	
	public HotspotDirectionalModel(int len, int hotspotlen) {
		this(len);
		this.hotspotlen = hotspotlen;
	}
	
	@Override
	public double computeConfidence(SessionMetadata md, DefinedTileView dtv, Direction d, List<NewTileKey> trace) {
		Direction hotDirection = this.getHotDirection(dtv,trace.get(trace.size()-1));
		if(d == hotDirection) {
			return 1.0001;
		} else {
			double confidence = super.computeConfidence(md,dtv,d, trace);
			if(confidence > 1.0) {
				return 1.0;
			}
			return confidence;
		}
	}
	
	// computes an ordering of all directions using confidence values
	@Override
	public List<DirectionPrediction> predictOrder(SessionMetadata md, DefinedTileView dtv,
			List<NewTileKey> htrace) {
		List<DirectionPrediction> order = new ArrayList<DirectionPrediction>();
		long start = System.currentTimeMillis();
		
		// see if hotspots will influence decision
		Direction hotdirection = null;
		if((htrace != null) && (htrace.size() > 0)) {
			hotdirection = this.getHotDirection(dtv, htrace.get(htrace.size()-1));
		}
		
		// for each direction, compute confidence
		this.getVotes(md,htrace); // check the history
		DirectionPrediction max = null;
		double maxscore = 0;
		for(Direction d : Direction.values()) {
			DirectionPrediction dp = new DirectionPrediction();
			dp.d = d;
			dp.confidence = 0;
			if(d == hotdirection) {
				//System.out.println("diverging in favor of hotspot: "+d);
				max = dp;
			}
			Double score = this.votes.get(d);
			if(score != null) {
				dp.confidence = score;
			}
			if(dp.confidence < defaultprob) {
				dp.confidence = defaultprob;
			}
			if(dp.confidence > maxscore) {
				maxscore = dp.confidence;
			}
			order.add(dp);
		}
		if(max != null) {
			max.confidence = maxscore + 1.0; // make this the top choice
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
	
	public void train(List<UserRequest> trace) {
		updateHotspots(trace);
	}
	
	public void finishTraining() {
		truncateHotspots();
	}
	
	public void truncateHotspots() {
		List<NewTileKey> topk = new ArrayList<NewTileKey>();
		List<Integer> scores = new ArrayList<Integer>();
		for(NewTileKey key : hotspots.keySet()) {
			int score = hotspots.get(key);
			//System.out.println("key: "+key+", count: "+score);

			if(topk.size() < this.hotspotlen) {
				topk.add(key);
				scores.add(score);
			} else {
				NewTileKey currentkey = key;
				for(int i = 0; i < topk.size(); i++) {
					if(score > scores.get(i)) {
						//store current to temporary variables
						NewTileKey temp = currentkey;
						int tempscore = score;
						
						// replace current with old
						score = scores.get(i);
						currentkey = topk.get(i);
						
						// replace old with current
						scores.set(i, tempscore);
						topk.set(i, temp);
					}
				}
			}
		}
		int end = this.hotspotlen;
		if(end > topk.size()) {
			end = topk.size();
		}
		topk = topk.subList(0, end);
		scores = scores.subList(0, end);
		hotspots.clear(); // reset the map
		for(int i = 0; i < end; i++) { // insert topk
			System.out.println("final key: "+topk.get(i)+", count: "+scores.get(i));
			hotspots.put(topk.get(i),scores.get(i));
		}
	}
	
	public void updateHotspots(List<UserRequest> trace) {
		for(UserRequest request: trace) {
			NewTileKey hottile = MarkovDirectionalModel.getKeyFromRequest(request);
			Integer count = hotspots.get(hottile);
			if(count == null) {
				hotspots.put(hottile, 1);
			} else {
				hotspots.put(hottile, count + 1);
			}
		}
	}
	
	protected Direction getHotDirection(DefinedTileView dtv, NewTileKey prevkey) {
		NewTileKey minkey = null;
		double mindist = 0;
		for(NewTileKey key : hotspots.keySet()) {
			double currdist = UtilityFunctions.manhattanDist(prevkey, key);
			if(currdist <= maxdistance) {
				if((minkey == null) || (currdist < mindist)) {
					mindist = currdist;
					minkey = key;
				}
			}
		}
		return getClose(dtv, prevkey, minkey);
	}
	
	protected Direction getClose(DefinedTileView dtv, NewTileKey prev, NewTileKey goal) {
		Direction d = null;
		if(goal == null) { // nothing was close enough
			return d;
		}

		double mindist = 0;
		for(Direction candidate : Direction.values()) {
			NewTileKey candidatekey = this.DirectionToTile(dtv,prev, candidate); // moving from prev
			if(candidatekey != null) {
				double currdist = UtilityFunctions.manhattanDist(goal, candidatekey);
				if((d == null) || (currdist < mindist)) { // does this make progress towards the goal?
					mindist = currdist;
					d = candidate;
				}
			}
		}
		return d;
	}
}
