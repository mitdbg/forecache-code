package backend.prediction.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.prediction.directional.MarkovDirectionalModel;
import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.Signatures;
import backend.util.Tile;
import backend.util.TileKey;

public class HistogramSignatureModel extends BasicModel {

	public HistogramSignatureModel(TileHistoryQueue ref, MemoryTileBuffer membuf, DiskTileBuffer diskbuf,ScidbTileInterface api, int len) {
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
		Tile orig = null;
		if(pkey != null) {
			orig = getTile(pkey);
		}
		
		TileKey ckey = this.DirectionToTile(prev, d);
		Tile candidate = null;
		if(ckey != null) {
			candidate = getTile(ckey);
		}
		
		if(candidate != null && orig != null) {
			confidence = Signatures.chiSquaredDistance(candidate.getHistogramSignature(), orig.getHistogramSignature());
			//System.out.println(ckey+" with confidence: "+dp.confidence);
		}
		if(confidence < defaultprob) {
			confidence = defaultprob;
		}
		return confidence;
	}
	
	@Override
	public Double computeConfidence(TileKey id, List<UserRequest> htrace) {
		return null;
	}
	
	@Override
	public Double computeDistance(TileKey id, List<UserRequest> htrace) {
		double distance = 0.0;
		Tile candidate = getTile(id);
		for(TileKey roiKey : roi) {
			Tile rtile = getTile(roiKey);
			distance += Signatures.chiSquaredDistance(candidate.getHistogramSignature(), rtile.getHistogramSignature());
		}

		if(distance < defaultprob) {
			distance = defaultprob;
		}
		return distance;
	}
}
