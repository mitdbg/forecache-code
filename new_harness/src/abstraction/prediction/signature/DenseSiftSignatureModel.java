package abstraction.prediction.signature;

import java.util.List;

import org.opencv.core.Mat;

import abstraction.enums.Model;
import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.structures.SessionMetadata;
import abstraction.tile.ColumnBasedNiceTile;

public class DenseSiftSignatureModel extends SiftSignatureModel{
	
	public DenseSiftSignatureModel(int len) {
		super(len);
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
	public double[] buildSignatureFromTile(ColumnBasedNiceTile tile) {
		return Signatures.buildDenseSiftSignature(tile, vocab, vocabSize);
	}
	
	@Override
	public void computeSignaturesInParallel(SessionMetadata md, DefinedTileView dtv, List<NewTileKey> ids) {
		//long a = System.currentTimeMillis();
		 List<double[]> sigs =  Signatures.buildDenseSiftSignaturesInParallel(dtv,ids, vocab, vocabSize);
		 for(int i = 0; i < sigs.size(); i++) {
			 histograms.put(ids.get(i), sigs.get(i));
		 }
		 //long b = System.currentTimeMillis();
		 //System.out.println("parallel:"+(b-a));
	}
	
	@Override
	public void computeSignaturesInParallel(List<ColumnBasedNiceTile> tiles) {
		//long a = System.currentTimeMillis();
		 List<double[]> sigs =  Signatures.buildDenseSiftSignaturesInParallel(tiles, vocab, vocabSize);
		 for(int i = 0; i < sigs.size(); i++) {
			 histograms.put(tiles.get(i).id, sigs.get(i));
		 }
		 //long b = System.currentTimeMillis();
		 //System.out.println("parallel:"+(b-a));
	}

}
