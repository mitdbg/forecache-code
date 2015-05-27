package backend.prediction.signature;

import java.util.List;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.OldScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.Model;
import backend.util.SignatureMap;
import backend.util.Signatures;
import backend.util.TileKey;

public class FilteredHistogramSignatureModel extends HistogramSignatureModel {

	public FilteredHistogramSignatureModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, 
			DiskNiceTileBuffer diskbuf,OldScidbTileInterface api, int len, SignatureMap sigMap) {
		super(ref,membuf,diskbuf,api,len, sigMap);
		this.m = Model.FHISTOGRAM;
	}
	
	@Override
	public double[] getSignature(TileKey id) {
		double[] sig = this.sigMap.getSignature(id, Model.FHISTOGRAM);
		if(sig == null) sig = new double[Signatures.defaultbins];
		return sig;
	}
	
	@Override
	public double computeConfidence(Direction d, List<TileKey> htrace) {
		double confidence = 0.0;
		TileKey pkey = htrace.get(htrace.size()-1);
		/*
		NiceTile orig = null;
		if(pkey != null) {
			orig = getTile(pkey);
		}
		*/
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
	public Double computeDistance(TileKey id, List<TileKey> htrace) {
		double distance = 0.0;
		//NiceTile candidate = getTile(id);
		for(TileKey roiKey : roi) {
			//NiceTile rtile = getTile(roiKey);
			distance += Signatures.chiSquaredDistance(getSignature(id),
					getSignature(roiKey));
			//distance += Signatures.chiSquaredDistance(Signatures.getFilteredHistogramSignature(candidate),
			//		Signatures.getFilteredHistogramSignature(rtile));
		}

		if(distance < defaultprob) {
			distance = defaultprob;
		}
		return distance;
	}
}
