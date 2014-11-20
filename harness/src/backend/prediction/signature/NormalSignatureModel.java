package backend.prediction.signature;

import java.util.List;
import utils.UserRequest;
import backend.disk.DiskNiceTileBuffer;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.prediction.directional.MarkovDirectionalModel;
import backend.util.Direction;
import backend.util.NiceTile;
import backend.util.Signatures;
import backend.util.Tile;
import backend.util.TileKey;

public class NormalSignatureModel extends BasicModel {

	public NormalSignatureModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len) {
		super(ref,membuf,diskbuf,api,len);
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(List<UserRequest> htrace) throws Exception {
		return super.predictOrder(htrace,false); // don't reverse the order here
	}
	
	@Override
	public double computeConfidence(Direction d, List<UserRequest> htrace) {
		double confidence = 0.0;
		UserRequest prev = htrace.get(htrace.size()-1);
		TileKey pkey = MarkovDirectionalModel.getKeyFromRequest(prev);
		//Tile orig = null;
		NiceTile orig = null;
		if(pkey != null) {
			//orig = getTile(pkey);
			orig = this.scidbapi.getNiceTile(pkey);
		}
		
		TileKey ckey = this.DirectionToTile(prev, d);
		//Tile candidate = null;
		NiceTile candidate = null;
		if(ckey != null) {
			//candidate = getTile(ckey);
			candidate = this.scidbapi.getNiceTile(ckey);
		}
		
		if(candidate != null && orig != null) {
			try{
				//TODO: too slow!!!!
				confidence = Signatures.chiSquaredDistance(Signatures.getNormalSignature(candidate),
						Signatures.getNormalSignature(orig));
			} catch(Exception e) {
				e.printStackTrace();
			}
			//System.out.println(ckey+" with confidence: "+dp.confidence);
		}
		if(confidence < defaultprob) {
			confidence = defaultprob;
		}
		return confidence;
	}
	
	@Override
	public Double computeConfidence(TileKey id, List<UserRequest> trace) {
		return null;
	}
	
	@Override
	public Double computeDistance(TileKey id, List<UserRequest> trace) {
		double distance = 0.0;
		NiceTile candidate = getTile(id);
		for(TileKey roiKey : roi) {
			NiceTile rtile = getTile(roiKey);
			try{
				distance += Signatures.chiSquaredDistance(Signatures.getNormalSignature(candidate),
						Signatures.getNormalSignature(rtile));
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		if(distance < defaultprob) {
			distance = defaultprob;
		}
		return distance;
	}
}
