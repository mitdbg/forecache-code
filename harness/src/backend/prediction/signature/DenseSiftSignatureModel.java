package backend.prediction.signature;

import org.opencv.core.Mat;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.OldScidbTileInterface;
import backend.disk.TileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.prediction.TileHistoryQueue;
import backend.util.Model;
import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.SignatureMap;
import backend.util.Signatures;
import backend.util.TileKey;

public class DenseSiftSignatureModel extends SiftSignatureModel{
	
	public DenseSiftSignatureModel(TileHistoryQueue ref, NiceTileBuffer membuf, 
			NiceTileBuffer diskbuf,TileInterface api, int len,
			SignatureMap sigMap) {
		super(ref,membuf,diskbuf,api,len, sigMap);
		this.m = Model.DSIFT;
	}
	
	/*
	@Override
	public double[] getSignature(TileKey id) {
		double[] sig = this.sigMap.getSignature(id, Model.DSIFT);
		if (sig == null) sig = new double[defaultVocabSize];
		return sig;
	}
	*/
	@Override
	public double[] buildSignatureFromMat(Mat d) {
		return Signatures.buildDenseSiftSignature(d, vocab, defaultVocabSize);
	}
	
	@Override
	public double[] buildSignatureFromKey(TileKey id) {
		//NiceTile tile = getTile(id);
		NiceTile tile = diskbuf.getTile(id);
		return Signatures.buildDenseSiftSignature(tile, vocab, defaultVocabSize);
	}

}
