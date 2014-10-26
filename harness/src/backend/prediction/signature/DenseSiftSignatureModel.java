package backend.prediction.signature;

import org.opencv.core.Mat;

import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryTileBuffer;
import backend.prediction.TileHistoryQueue;
import backend.util.Signatures;
import backend.util.TileKey;

public class DenseSiftSignatureModel extends SiftSignatureModel{
	
	public DenseSiftSignatureModel(TileHistoryQueue ref, MemoryTileBuffer membuf, DiskTileBuffer diskbuf,ScidbTileInterface api, int len) {
		super(ref,membuf,diskbuf,api,len);
	}
	
	@Override
	public double[] buildSignatureFromMat(Mat d) {
		return Signatures.buildDenseSiftSignature(d, vocab, defaultVocabSize);
	}
	
	@Override
	public double[] buildSignatureFromKey(TileKey id) {
		return Signatures.buildDenseSiftSignature(id, vocab, defaultVocabSize);
	}

}
