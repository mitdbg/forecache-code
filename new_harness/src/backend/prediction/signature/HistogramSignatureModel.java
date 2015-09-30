package backend.prediction.signature;

import java.util.List;

import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import abstraction.prediction.DefinedTileView;
import abstraction.prediction.SessionMetadata;
import abstraction.util.Direction;
import abstraction.util.Model;
import abstraction.util.SignatureType;
import abstraction.util.Signatures;
import abstraction.util.NewTileKey;

public class HistogramSignatureModel extends BasicSignatureModel {

	public HistogramSignatureModel(int len) {
		super(len);
		this.m = Model.HISTOGRAM;
	}
	
	public double[] getSignature(SessionMetadata md, DefinedTileView dtv, NewTileKey id) {
		double[] sig = dtv.sigMap.getSignature(id, SignatureType.HISTOGRAM);
		if(sig == null) sig = new double[Signatures.defaultbins];
		return sig;
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(SessionMetadata md, DefinedTileView dtv,
			List<NewTileKey> htrace) {
		return super.predictOrder(md,dtv,htrace,false); // don't reverse the order here
	}
	
	@Override
	public double computeConfidence(SessionMetadata md, DefinedTileView dtv,
			Direction d, List<NewTileKey> htrace) {
		double confidence = 0.0;
		NewTileKey pkey = htrace.get(htrace.size()-1);
		/*
		NiceTile orig = null;
		if(pkey != null) {
			orig = getTile(pkey);
		}*/
		
		NewTileKey ckey = this.DirectionToTile(dtv,pkey, d);
		/*
		NiceTile candidate = null;
		if(ckey != null) {
			candidate = getTile(ckey);
		}
		
		if(candidate != null && orig != null) {
		*/
		for(NewTileKey roiKey : roi) {
			confidence += Signatures.chiSquaredDistance(getSignature(md,dtv,ckey),
					getSignature(md,dtv,roiKey));
		}
			//System.out.println(ckey+" with confidence: "+dp.confidence);
		//}
		if(confidence < defaultprob) {
			confidence = defaultprob;
		}
		return confidence;
	}
	
	@Override
	public Double computeConfidence(SessionMetadata md, DefinedTileView dtv,
			NewTileKey id, List<NewTileKey> htrace) {
		return null;
	}
	
	@Override
	public Double computeDistance(SessionMetadata md, DefinedTileView dtv, 
			NewTileKey id, List<NewTileKey> htrace) {
		double distance = 0.0;
		//NiceTile candidate = getTile(id);
		for(NewTileKey roiKey : roi) {
			//NiceTile rtile = getTile(roiKey);
			distance += Signatures.chiSquaredDistance(getSignature(md,dtv,id),
					getSignature(md,dtv,roiKey));
		}

		if(distance < defaultprob) {
			distance = defaultprob;
		}
		return distance;
	}
}
