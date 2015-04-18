package backend.prediction.directional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.UserRequest;
import utils.UtilityFunctions;
import backend.disk.DiskNiceTileBuffer;
import backend.disk.DiskTileBuffer;
import backend.disk.OldScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.TileKey;

public class MomentumDirectionalModel extends BasicModel {
	protected Map<Character,Double> votes;
	
	public MomentumDirectionalModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,OldScidbTileInterface api, int len){
		super(ref,membuf,diskbuf,api,len);
		this.votes = new HashMap<Character,Double>();
		this.useDistanceCorrection = false;
	}
	
	// computes an ordering of all directions using confidence values
	@Override
	public List<DirectionPrediction> predictOrder(List<TileKey> htrace) {
		getVotes(htrace); // update our voting system;
		return super.predictOrder(htrace,true);
	}
	
	@Override
	public Double computeConfidence(TileKey id, List<TileKey> trace) {
		getVotes(trace); // update our voting system;
		List<TileKey> traceCopy = new ArrayList<TileKey>();
		traceCopy.addAll(trace);
		TileKey prev = traceCopy.get(traceCopy.size() - 1);
		//List<TileKey> path = UtilityFunctions.buildPath2(prev, id); // build a path to this key
		List<TileKey> path = UtilityFunctions.buildPath(prev, id); // build a path to this key
		return computeConfidenceForPath(path,traceCopy);
	}
	
	@Override
	public Double computeDistance(TileKey id, List<TileKey> trace) {
		return null;
	}
	
	// probability of entire path to this tile
	public Double computeConfidenceForPath(List<TileKey> path, List<TileKey> traceCopy) {
		List<Direction> dirPath = UtilityFunctions.buildDirectionPath(path);
		//System.out.print("path for "+path.get(path.size()-1)+":");
		//for(Direction d : dirPath) {
		//	System.out.print(d+",");
		//}
		//System.out.println();
		
		if(dirPath.size() == 1) {
			return Math.log(computeConfidence(dirPath.get(0),traceCopy));
		}
		double prob = 0;
		for(int i = 0; i < dirPath.size(); i++) {
			Direction d = dirPath.get(i);
			prob += Math.log(computeConfidence(d,traceCopy)); // log probabilities
			traceCopy.remove(0);
			traceCopy.add(path.get(i+1));
		}
		//System.out.println("prob for "+path.get(path.size()-1)+": "+prob);
		return prob;
	}
	
	// average confidence across all directions in the path for this tile
	public Double computeConfidenceForPath2(List<TileKey> path, List<TileKey> traceCopy) {
		List<Direction> dirPath = UtilityFunctions.buildDirectionPath(path);
		if(dirPath.size() == 1) {
			return computeConfidence(dirPath.get(0),traceCopy);
		}
		double prob = 0;
		int count = dirPath.size();
		for(int i = 0; i < dirPath.size(); i++) {
			Direction d = dirPath.get(i);
			prob += computeConfidence(d,traceCopy);
			traceCopy.remove(0);
			traceCopy.add(path.get(i+1));
		}
		return prob / count; // just return the average
	}
	
	@Override
	public double computeConfidence(Direction d, List<TileKey> trace) {
		Double score = this.votes.get(d.getVal().charAt(0));
		double confidence = 0.0;
		if(score != null) {
			confidence = score;
		}
		if(confidence < defaultprob) {
			confidence = defaultprob;
		}
		//System.out.println("confidence for direction "+d+": "+confidence);
		return confidence;
	}
	
	public void getVotes(List<TileKey> trace) {
		this.votes.clear();
		if(this.history == null) {
			return;
		}
		
		double currvotevalue = 1.0;
		double sum = 0;
		
		//distribute votes based on recent history
		String dirstring = buildDirectionStringFromKey(trace);
		for(int i =dirstring.length() - 1; i >= 0; i--) {
			char d = dirstring.charAt(i);
			Double vote = this.votes.get(d);
			if(vote == null) {
				this.votes.put(d,currvotevalue);
			} else {
				this.votes.put(d,vote+currvotevalue);
			}
			sum += currvotevalue;
			currvotevalue /= 2;
		}
		
		/*
		// fill in some blanks
		double remainder = Direction.values().length - this.votes.size();
		double toShare = sum / 10; // only add a small fraction to the alloted votes
		if(remainder > 0) {
			// add extra votes
			sum += toShare;
			//share this extra vote amongst the remaining directions
			double toAdd = toShare / remainder;
			for(Direction d : Direction.values()) {
				Character dc = d.getVal().charAt(0);
				if(!this.votes.containsKey(dc)) {
					votes.put(dc, toAdd);
				}
			}
		}
		*/
		
		//normalize
		for(Character d: this.votes.keySet()) {
			this.votes.put(d, this.votes.get(d) / sum);
			//System.out.println("votes for "+d+": "+this.votes.get(d));
		}
	}
}
