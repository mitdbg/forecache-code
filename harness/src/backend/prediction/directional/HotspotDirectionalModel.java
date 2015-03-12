package backend.prediction.directional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryTileBuffer;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.TileKey;

import utils.UserRequest;

public class HotspotDirectionalModel extends MomentumDirectionalModel {
	protected Map<TileKey,Integer> hotspots;
	public static final int defaulthotspotlen = 5;
	public static final double maxdistance = 2.0;
	protected int hotspotlen;
	
	public HotspotDirectionalModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len) {
		super(ref,membuf,diskbuf,api,len);
		this.hotspots = new HashMap<TileKey,Integer>();
		this.hotspotlen = defaulthotspotlen;
	}
	
	public HotspotDirectionalModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len, int hotspotlen) {
		this(ref,membuf,diskbuf,api,len);
		this.hotspotlen = hotspotlen;
	}
	
	@Override
	public double computeConfidence(Direction d, List<TileKey> trace) {
		Direction hotDirection = this.getHotDirection(trace.get(trace.size()-1));
		if(d == hotDirection) {
			return 1.0001;
		} else {
			double confidence = super.computeConfidence(d, trace);
			if(confidence > 1.0) {
				return 1.0;
			}
			return confidence;
		}
	}
	
	// computes an ordering of all directions using confidence values
	@Override
	public List<DirectionPrediction> predictOrder(List<TileKey> htrace) {
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
	
	protected Direction getHotDirection(TileKey prevkey) {
		TileKey minkey = null;
		double mindist = 0;
		for(TileKey key : hotspots.keySet()) {
			double currdist = prevkey.getDistance(key);
			if(currdist <= maxdistance) {
				if((minkey == null) || (currdist < mindist)) {
					mindist = currdist;
					minkey = key;
				}
			}
		}
		return getClose(prevkey, minkey);
	}
	
	protected Direction getClose(TileKey prev, TileKey goal) {
		Direction d = null;
		if(goal == null) { // nothing was close enough
			return d;
		}

		double mindist = 0;
		for(Direction candidate : Direction.values()) {
			TileKey candidatekey = this.DirectionToTile(prev, candidate); // moving from prev
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
