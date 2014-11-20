package backend.prediction.directional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.UserRequest;
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
		String dirstring = buildDirectionStringFromKey(trace);
		for(int i =dirstring.length() - 1; i >= 0; i--) {
			char d = dirstring.charAt(i);
			Double vote = votes.get(d);
			if(vote == null) {
				votes.put(d,currvotevalue);
			} else {
				votes.put(d,vote+currvotevalue);
			}
			currvotevalue /= 2;
		}
	}
}
