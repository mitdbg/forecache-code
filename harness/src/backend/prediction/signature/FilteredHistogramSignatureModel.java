package backend.prediction.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import utils.DBInterface;
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
import backend.prediction.directional.MarkovDirectionalModel;
import backend.util.Direction;
import backend.util.NiceTile;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.Signatures;
import backend.util.Tile;
import backend.util.TileKey;

public class FilteredHistogramSignatureModel extends HistogramSignatureModel {

	public FilteredHistogramSignatureModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len) {
		super(ref,membuf,diskbuf,api,len);
	}
	
	@Override
	public double computeConfidence(Direction d, List<UserRequest> htrace) {
		double confidence = 0.0;
		UserRequest prev = htrace.get(htrace.size()-1);
		TileKey pkey = MarkovDirectionalModel.getKeyFromRequest(prev);
		NiceTile orig = null;
		if(pkey != null) {
			orig = getTile(pkey);
		}
		
		TileKey ckey = this.DirectionToTile(prev, d);
		NiceTile candidate = null;
		if(ckey != null) {
			candidate = getTile(ckey);
		}
		
		if(candidate != null && orig != null) {
			confidence = Signatures.chiSquaredDistance(Signatures.getFilteredHistogramSignature(candidate),
					Signatures.getFilteredHistogramSignature(orig));
			//System.out.println(ckey+" with confidence: "+dp.confidence);
		}
		if(confidence < defaultprob) {
			confidence = defaultprob;
		}
		return confidence;
	}
	
	@Override
	public Double computeDistance(TileKey id, List<UserRequest> htrace) {
		double distance = 0.0;
		NiceTile candidate = getTile(id);
		for(TileKey roiKey : roi) {
			NiceTile rtile = getTile(roiKey);
			distance += Signatures.chiSquaredDistance(Signatures.getFilteredHistogramSignature(candidate),
					Signatures.getFilteredHistogramSignature(rtile));
		}

		if(distance < defaultprob) {
			distance = defaultprob;
		}
		return distance;
	}
}
