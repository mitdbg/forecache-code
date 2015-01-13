package backend.prediction.directional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.UserRequest;
import utils.UtilityFunctions;
import backend.disk.DiskNiceTileBuffer;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.TileKey;

public class MomentumDirectionalModel extends BasicModel {
	protected Map<Character,Double> votes;
	
	public MomentumDirectionalModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len){
		super(ref,membuf,diskbuf,api,len);
		this.votes = new HashMap<Character,Double>();
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
		return computeConfidenceForPath2(path,traceCopy);
	}
	
	@Override
	public Double computeDistance(TileKey id, List<TileKey> trace) {
		return null;
	}
	
	// probability of entire path to this tile
	public Double computeConfidenceForPath(List<TileKey> path, List<TileKey> traceCopy) {
		List<Direction> dirPath = UtilityFunctions.buildDirectionPath(path);
		if(dirPath.size() == 1) {
			return computeConfidence(dirPath.get(0),traceCopy);
		}
		double prob = 0;
		for(int i = 0; i < dirPath.size(); i++) {
			Direction d = dirPath.get(i);
			prob += Math.log(computeConfidence(d,traceCopy)); // log probabilities
			traceCopy.remove(0);
			traceCopy.add(path.get(i+1));
		}
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
		Double score = this.votes.get(d);
		double confidence = 0.0;
		if(score != null) {
			confidence = score;
		}
		if(confidence < defaultprob) {
			confidence = defaultprob;
		}
		return confidence;
	}
	
	public void getVotes(List<TileKey> trace) {
		votes.clear();
		if(this.history == null) {
			return;
		}
		
		double currvotevalue = 1.0;
		double sum = 0;
		
		//disribute votes based on recent history
		String dirstring = buildDirectionStringFromKey(trace);
		for(int i =dirstring.length() - 1; i >= 0; i--) {
			char d = dirstring.charAt(i);
			Double vote = votes.get(d);
			if(vote == null) {
				votes.put(d,currvotevalue);
			} else {
				votes.put(d,vote+currvotevalue);
			}
			sum += currvotevalue;
			currvotevalue /= 2;
		}
		
		//normalize
		for(Character d: votes.keySet()) {
			votes.put(d, votes.get(d) / sum);
		}
	}
}
