package abstraction.prediction.signature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;


import abstraction.enums.Direction;
import abstraction.enums.Model;
import abstraction.prediction.DirectionPrediction;
import abstraction.prediction.signature.Signatures.ConcurrentKDTree;
import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.structures.SessionMetadata;
import abstraction.tile.ColumnBasedNiceTile;
import abstraction.util.DBInterface;
import abstraction.util.UtilityFunctions;

public class SiftSignatureModel extends BasicSignatureModel {
	
	public static int defaultVocabSize = 100;
	public int vocabSize;
	protected boolean haveRealRoi = false;
	ConcurrentKDTree<Integer> vocab = null;
	protected Map<NewTileKey,double[]> histograms;

	public SiftSignatureModel(int len) {
		super(len);
		this.histograms = new HashMap<NewTileKey,double[]>();
		this.vocabSize = defaultVocabSize;
		this.m = Model.SIFT;
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(SessionMetadata md, DefinedTileView dtv,
			List<NewTileKey> htrace) {
		updateRoi(md,dtv); // make sure we're using the most recent ROI
		return super.predictOrder(md,dtv,htrace,false); // don't reverse the order here
	}
	
	@Override
	public double computeConfidence(SessionMetadata md, DefinedTileView dtv,
			Direction d, List<NewTileKey> trace) {
		double confidence = 0.0;
		NewTileKey prev = trace.get(trace.size() - 1);
		NewTileKey ckey = this.DirectionToTile(dtv,prev, d);
		if(ckey == null) {
			return defaultprob;
		}
		//double[] vocabhist = buildSignature(ckey);
		for(NewTileKey roiKey : roi) {
			//double[] roihist = histograms.get(roiKey);
			//confidence += Signatures.chiSquaredDistance(vocabhist, roihist);
			confidence += Signatures.chiSquaredDistance(getSignature(md,dtv,ckey),
					getSignature(md,dtv,roiKey));
		}

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
		//double[] vocabhist = buildSignature(id);
		for(NewTileKey roiKey : roi) {
			//double[] roihist = histograms.get(roiKey);
			//distance += Signatures.chiSquaredDistance(vocabhist, roihist);
			distance += Signatures.chiSquaredDistance(getSignature(md,dtv,id),
					getSignature(md,dtv,roiKey));
		}

		if(distance < defaultprob) {
			distance = defaultprob;
		}
		return distance;
	}
	
	@Override
	public void computeSignaturesInParallel(SessionMetadata md, DefinedTileView dtv, List<NewTileKey> ids) {
		//long a = System.currentTimeMillis();
		 List<double[]> sigs =  Signatures.buildSiftSignaturesInParallel(dtv,ids, vocab, vocabSize);
		 for(int i = 0; i < sigs.size(); i++) {
			 histograms.put(ids.get(i), sigs.get(i));
		 }
		 //long b = System.currentTimeMillis();
		 //System.out.println("parallel:"+(b-a));
	}
	
	@Override
	public double[] getSignature(SessionMetadata md, DefinedTileView dtv, NewTileKey id) {
		//double[] sig = this.sigMap.getSignature(id, Model.SIFT);
		double[] sig = buildSignature(dtv, id);
		return sig;
	}
	
	// stores the histograms
	public double[] buildSignature(DefinedTileView dtv, NewTileKey id) {
		double[] signature = histograms.get(id);
		if(signature == null) {
			signature = buildSignatureFromKey(dtv,id);
			histograms.put(id, signature);
		}
		return signature;
	}
	
	//TODO: override these to implement a new image signature!
	public double[] buildSignatureFromMat(Mat d) {
		return Signatures.buildSiftSignature(d, vocab, vocabSize);
	}
	
	public double[] buildSignatureFromKey(DefinedTileView dtv, NewTileKey id) {
		//NiceTile tile = getTile(id);
		ColumnBasedNiceTile tile = new ColumnBasedNiceTile(id);
		dtv.nti.getTile(dtv.v, dtv.ts, tile);
		return Signatures.buildSiftSignature(tile, vocab, vocabSize);
	}
	
	
	@Override
	public void updateRoi(SessionMetadata md, DefinedTileView dtv) {
		//long s = System.currentTimeMillis();
		if(haveRealRoi && !md.history.newRoi()) return; // nothing to update
		else if (!haveRealRoi && md.history.newRoi()) haveRealRoi = true; // now we have a real ROI
		
		List<NewTileKey> roi = md.history.getLastRoi();
		List<NewTileKey> finalRois = new ArrayList<NewTileKey>();
		histograms.clear(); // these histograms are now obsolete
		this.roi = roi;
		List<Mat> all_descriptors = new ArrayList<Mat>();
		int rows = 0;
		int cols = 0;
		for(NewTileKey id : roi) { // for each tile in the ROI
			long a = System.currentTimeMillis();
			//NiceTile t = getTile(id);
			ColumnBasedNiceTile t = new ColumnBasedNiceTile(id);
			dtv.nti.getTile(dtv.v, dtv.ts, t);
			//System.out.println("size of t: "+t.getSize()+", time to get t: "+(System.currentTimeMillis()-a));
			//Mat d = Signatures.getSiftDescriptorsForImage(getTile(id));
			Mat d = Signatures.getSiftDescriptorsForImage(t);
			if(d.rows() > 0) {
				rows += d.rows();
				all_descriptors.add(d); // better have the same number of cols!
				finalRois.add(id);
			}
		}
		roi = finalRois;
		// merge into a single matrix
		if(all_descriptors.size() > 0) {
			cols = all_descriptors.get(0).cols();
			Mat finalMatrix = Mat.zeros(rows,cols, all_descriptors.get(0).type());
			//System.out.println("finalMatrix=("+rows+","+cols+")");
			int curr = 0;
			for(int i = 0; i < all_descriptors.size(); i++) {
				Mat d = all_descriptors.get(i);
				int r = d.rows();
				d.copyTo(finalMatrix.submat(curr,curr+r,0,cols));
				curr += r;
			}
			// these clusters are our visual words
			// each center represents the center of a word
			Mat centers = Signatures.getKmeansCenters(finalMatrix, defaultVocabSize);
			vocab = Signatures.buildConcurrentKDTree(centers); // used to find nearest neighbor fast
			vocabSize = centers.rows();
/*
			if(vocabSize < defaultVocabSize) {
				for(int i = 0; i < roi.size(); i++) {
					NewTileKey rkey = roi.get(i);
					System.out.print(rkey.buildTileStringForFile()+ "-");
					System.out.print(all_descriptors.get(i).rows()+" ");
				}
				System.out.println();
			}
*/

			// now go back through and build histograms for each ROI
			if(all_descriptors.size() < 3) {
				for(int i = 0; i < all_descriptors.size(); i++) {
					Mat d = all_descriptors.get(i);
					NewTileKey id = roi.get(i);
					double[] signature = buildSignatureFromMat(d);
					histograms.put(id, signature); // save for later use
				}
			} else {
				Signatures.buildSiftSignaturesInParallel(dtv, roi, vocab, vocabSize);
			}
			
		}
		//System.out.println("time updating roi's: "+(System.currentTimeMillis()-s));
	}
	
	public static void main(String[] args) {
		/*
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		String idstr = "[0, 0]";
		int zoom = 1;
		int[] tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		NewTileKey id = new NewTileKey(tile_id,zoom);
		int vocabSize = defaultVocabSize;
		OldScidbTileInterface scidbapi = new OldScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		Mat descriptors = Signatures.getSiftDescriptorsForImage(scidbapi.getNiceTile(id));
		System.out.println("descriptors: "+descriptors.rows()+","+descriptors.cols());
		Mat centers = Signatures.getKmeansCenters(descriptors, vocabSize);
		System.out.println("centers dimensions: "+centers.dims()+", ("+centers.rows()+","+centers.cols()+")");
		//for(int i = 0; i < centers.cols(); i++) {
		//	System.out.print(centers.get(0, i)[0]+" ");
		//	System.out.println();
		//}
		
		ConcurrentKDTree<Integer> vocab = Signatures.buildConcurrentKDTree(centers);
		double[] hist = Signatures.buildSiftSignature(descriptors, vocab, defaultVocabSize);
		for(int i = 0; i < hist.length; i++) {
			System.out.print(" "+hist[i]);
		}
		System.out.println();
		
		Signatures.writeMat(Signatures.denseSiftString,descriptors, id);
		descriptors.release();
		Mat test = Signatures.readMat(Signatures.siftString,id);
		System.out.println(test.get(0, 120)[0]+","+test.get(27,59)[0]);
		//System.out.println(descriptors.get(0, 120)[0]+","+descriptors.get(27,59)[0]);
		 */
	}
}
