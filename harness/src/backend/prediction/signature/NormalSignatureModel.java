package backend.prediction.signature;

import java.util.List;
import backend.disk.DiskNiceTileBuffer;
import backend.disk.OldScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.Model;
import backend.util.SignatureMap;
import backend.util.Signatures;
import backend.util.TileKey;

public class NormalSignatureModel extends BasicSignatureModel {

	public NormalSignatureModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf,
			DiskNiceTileBuffer diskbuf,OldScidbTileInterface api, int len,
			SignatureMap sigMap) {
		super(ref,membuf,diskbuf,api,len,sigMap);
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(List<TileKey> htrace) {
		return super.predictOrder(htrace,false); // don't reverse the order here
	}
	
	@Override
	public double computeConfidence(Direction d, List<TileKey> htrace) {
		double confidence = 0.0;
		TileKey pkey = htrace.get(htrace.size()-1);
		//Tile orig = null;
		/*
		 NiceTile orig = null;
		if(pkey != null) {
			//orig = getTile(pkey);
			orig = this.scidbapi.getNiceTile(pkey);
		}
		*/
		TileKey ckey = this.DirectionToTile(pkey, d);
		/*
		Tile candidate = null;
		NiceTile candidate = null;
		if(ckey != null) {
			//candidate = getTile(ckey);
			candidate = this.scidbapi.getNiceTile(ckey);
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
	
	public double[] getSignature(TileKey id) {
		double[] sig = this.sigMap.getSignature(id, Model.NORMAL);
		if(sig == null) sig = new double[2];
		return sig;
	}
	
	@Override
	public Double computeConfidence(TileKey id, List<TileKey> trace) {
		return null;
	}
	
	@Override
	public Double computeDistance(TileKey id, List<TileKey> trace) {
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
