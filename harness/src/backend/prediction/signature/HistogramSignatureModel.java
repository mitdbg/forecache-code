package backend.prediction.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;
import backend.BuildSignaturesOffline;
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
import backend.util.Model;
import backend.util.NiceTile;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.SignatureMap;
import backend.util.Signatures;
import backend.util.Tile;
import backend.util.TileKey;

public class HistogramSignatureModel extends BasicSignatureModel {

	public HistogramSignatureModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, 
			DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len,
			SignatureMap sigMap) {
		super(ref,membuf,diskbuf,api,len, sigMap);
	}
	
	public double[] getSignature(TileKey id) {
		double[] sig = this.sigMap.getSignature(id, Model.HISTOGRAM);
		if(sig == null) {
			NiceTile tile = getTile(id);
			sig = Signatures.getNormalSignature(tile);
			this.sigMap.updateSignature(id, Model.HISTOGRAM, sig); // add to signature map
			
			// TODO: might be too slow to always write the whole structure to disk
			this.sigMap.save(BuildSignaturesOffline.defaultFilename); // save changes to disk
		}
		return sig;
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(List<TileKey> htrace) {
		return super.predictOrder(htrace,false); // don't reverse the order here
	}
	
	@Override
	public double computeConfidence(Direction d, List<TileKey> htrace) {
		double confidence = 0.0;
		TileKey pkey = htrace.get(htrace.size()-1);
		/*
		NiceTile orig = null;
		if(pkey != null) {
			orig = getTile(pkey);
		}*/
		
		TileKey ckey = this.DirectionToTile(pkey, d);
		/*
		NiceTile candidate = null;
		if(ckey != null) {
			candidate = getTile(ckey);
		}
		
		if(candidate != null && orig != null) {
		*/
		for(TileKey roiKey : roi) {
			confidence += Signatures.chiSquaredDistance(getSignature(ckey),
					getSignature(roiKey));
		}
			//System.out.println(ckey+" with confidence: "+dp.confidence);
		//}
		if(confidence < defaultprob) {
			confidence = defaultprob;
		}
		return confidence;
	}
	
	@Override
	public Double computeConfidence(TileKey id, List<TileKey> htrace) {
		return null;
	}
	
	@Override
	public Double computeDistance(TileKey id, List<TileKey> htrace) {
		double distance = 0.0;
		//NiceTile candidate = getTile(id);
		for(TileKey roiKey : roi) {
			//NiceTile rtile = getTile(roiKey);
			distance += Signatures.chiSquaredDistance(getSignature(id),
					getSignature(roiKey));
		}

		if(distance < defaultprob) {
			distance = defaultprob;
		}
		return distance;
	}
}
