package backend.prediction.directional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.prediction.directional.MarkovDirectionalModel.MDMNode;
import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.TileKey;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;

public class HotspotDirectionalModel extends MomentumDirectionalModel {
	protected Map<TileKey,Integer> hotspots;
	public static final int defaulthotspotlen = 5;
	public static final double maxdistance = 2.0;
	protected int hotspotlen;
	
	public HotspotDirectionalModel(TileHistoryQueue ref, int hotspotlen) {
		super(ref);
		this.hotspots = new HashMap<TileKey,Integer>();
		this.hotspotlen = hotspotlen;
	}
	
	public void train(List<UserRequest> trace) {
		updateHotspots(trace);
	}
	
	public void finishTraining() {
		truncateHotspots();
	}
	
	public void truncateHotspots() {
		List<TileKey> topk = new ArrayList<TileKey>();
		List<Integer> scores = new ArrayList<Integer>();
		for(TileKey key : hotspots.keySet()) {
			int score = hotspots.get(key);
			//System.out.println("key: "+key+", count: "+score);

			if(topk.size() < this.hotspotlen) {
				topk.add(key);
				scores.add(score);
			} else {
				TileKey currentkey = key;
				for(int i = 0; i < topk.size(); i++) {
					if(score > scores.get(i)) {
						//store current to temporary variables
						TileKey temp = currentkey;
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
			TileKey hottile = MarkovDirectionalModel.getKeyFromRequest(request);
			Integer count = hotspots.get(hottile);
			if(count == null) {
				hotspots.put(hottile, 1);
			} else {
				hotspots.put(hottile, count + 1);
			}
		}
	}
	
	// computes an ordering of all directions using confidence values
	@Override
	public List<DirectionPrediction> predictOrder(List<UserRequest> htrace) {
		List<DirectionPrediction> order = new ArrayList<DirectionPrediction>();
		long start = System.currentTimeMillis();
		
		// see if hotspots will influence decision
		Direction hotdirection = null;
		if((htrace != null) && (htrace.size() > 0)) {
			hotdirection = this.getHotDirection(htrace.get(htrace.size()-1));
		}
		
		// for each direction, compute confidence
		this.getVotes(htrace); // check the history
		DirectionPrediction max = null;
		double maxscore = 0;
		for(Direction d : Direction.values()) {
			DirectionPrediction dp = new DirectionPrediction();
			dp.d = d;
			dp.confidence = 0;
			if(d == hotdirection) {
				System.out.println("diverging in favor of hotspot: "+d);
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
		System.out.println("time to predict order: "+(end-start)+"ms");
		return order;
	}
	
	protected Direction getHotDirection(UserRequest prev) {
		TileKey minkey = null;
		double mindist = 0;
		TileKey prevkey = MarkovDirectionalModel.getKeyFromRequest(prev);
		for(TileKey key : hotspots.keySet()) {
			double currdist = prevkey.getDistance(key);
			if(currdist <= maxdistance) {
				if((minkey == null) || (currdist < mindist)) {
					mindist = currdist;
					minkey = key;
				}
			}
		}
		return getClose(prev, minkey);
	}
	
	protected Direction getClose(UserRequest prev, TileKey goal) {
		Direction d = null;
		if(goal == null) { // nothing was close enough
			return d;
		}

		double mindist = 0;
		for(Direction candidate : Direction.values()) {
			TileKey candidatekey = this.directionToTile(prev, candidate); // moving from prev
			if(candidatekey != null) {
				double currdist = goal.getDistance(candidatekey);
				if((d == null) || (currdist < mindist)) { // does this make progress towards the goal?
					mindist = currdist;
					d = candidate;
				}
			}
		}
		return d;
	}
}
