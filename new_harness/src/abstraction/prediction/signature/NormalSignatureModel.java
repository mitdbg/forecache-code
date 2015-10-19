package abstraction.prediction.signature;

import java.util.List;

import abstraction.enums.Direction;
import abstraction.enums.Model;
import abstraction.enums.SignatureType;
import abstraction.prediction.DirectionPrediction;
import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.structures.SessionMetadata;

public class NormalSignatureModel extends BasicSignatureModel {

	public NormalSignatureModel(int len) {
		super(len);
		this.m = Model.NORMAL;
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(SessionMetadata md, DefinedTileView dtv, List<NewTileKey> htrace) {
		return super.predictOrder(md,dtv,htrace,false); // don't reverse the order here
	}
	
	@Override
	public double computeConfidence(SessionMetadata md, DefinedTileView dtv,
			Direction d, List<NewTileKey> htrace) {
		double confidence = 0.0;
		NewTileKey pkey = htrace.get(htrace.size()-1);
		//Tile orig = null;
		/*
		 NiceTile orig = null;
		if(pkey != null) {
			//orig = getTile(pkey);
			orig = this.scidbapi.getNiceTile(pkey);
		}
		*/
		NewTileKey ckey = this.DirectionToTile(dtv,pkey, d);
		/*
		Tile candidate = null;
		NiceTile candidate = null;
		if(ckey != null) {
			//candidate = getTile(ckey);
			candidate = this.scidbapi.getNiceTile(ckey);
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
	
	public double[] getSignature(SessionMetadata md, DefinedTileView dtv, NewTileKey id) {
		double[] sig = dtv.sigMap.getSignature(id, SignatureType.NORMAL);
		if(sig == null) sig = new double[2];
		return sig;
	}
	
	@Override
	public Double computeConfidence(SessionMetadata md, DefinedTileView dtv,
			NewTileKey id, List<NewTileKey> trace) {
		return null;
	}
	
	@Override
	public Double computeDistance(SessionMetadata md, DefinedTileView dtv,
			NewTileKey id, List<NewTileKey> trace) {
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
