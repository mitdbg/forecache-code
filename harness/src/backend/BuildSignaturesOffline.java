package backend;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import utils.DBInterface;

import edu.wlu.cs.levy.CG.KDTree;

import backend.disk.DiskNiceTileBuffer;
import backend.prediction.signature.SiftSignatureModel;
import backend.util.Model;
import backend.util.NiceTile;
import backend.util.SignatureMap;
import backend.util.Signatures;
import backend.util.TileKey;

public class BuildSignaturesOffline {
	public static String defaultFilename = "sigMap_k100.ser";
	public static void main(String[] args) throws Exception {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.out.println("populating disk buffer");
		DiskNiceTileBuffer diskbuf = new DiskNiceTileBuffer(DBInterface.nice_tile_cache_dir,DBInterface.hashed_query,DBInterface.threshold);
		System.out.println("done populating buffer... building signatures");
		SignatureMap map = buildSignatures(new Model[]{Model.HISTOGRAM,Model.NORMAL,Model.SIFT,Model.DSIFT,Model.FHISTOGRAM}, diskbuf);
		System.out.println("done building signatures... saving to disk");
		map.save(defaultFilename);
		System.out.println("done saving to disk... reading from disk");
		map = SignatureMap.getFromFile(defaultFilename);
		System.out.println("Map size: "+map.size());
/*
		 List<TileKey> keys = new ArrayList<TileKey>(diskbuf.getAllTileKeys());
		for(int i = 0; i < 10; i++) {
			double[] sig = map.getSignature(keys.get(i), Model.SIFT);
			for(int j = 0; j < 10; j++) {
				System.out.print(sig[j]+" ");
			}
			System.out.println();
		}
		*/
	}
	
	public static SignatureMap buildSignatures(Model[] models, DiskNiceTileBuffer buffer) {
		SignatureMap mp = new SignatureMap(models);
		
		// List of tile id's instead of a Set
		// used by SIFT
		List<TileKey> keys = new ArrayList<TileKey>();
		
		// SIFT-specific variables
		List<Mat> siftDescriptors = new ArrayList<Mat>();
		int sift_rows = 0;
		
		// denseSIFT-specific variables
		List<Mat> denseSiftDescriptors = new ArrayList<Mat>();
		int dsift_rows = 0;
		
		// for every tile
		for(TileKey id : buffer.getAllTileKeys()) {
			keys.add(id);
			NiceTile tile = buffer.getTile(id);
			// if we are making this signature, compute it for this tile
			for(int i = 0; i < models.length; i++) {
				Model label = models[i];
				double[] sig = null;
				Mat d;
				switch(label) {
					case NORMAL: // compute
						sig = Signatures.getNormalSignature(tile);
						break;
					case HISTOGRAM: // compute
						sig = Signatures.getHistogramSignature(tile);
						break;
					case FHISTOGRAM: // compute
						sig = Signatures.getFilteredHistogramSignature(tile);
						break;
					case SIFT: // compute
						d = Signatures.getSiftDescriptorsForImage(tile);
						if(d.rows() > 0) {
							sift_rows += d.rows();
							siftDescriptors.add(d); // better have the same number of cols!
						}
						break;
					case DSIFT: // compute
						d = Signatures.getDenseSiftDescriptorsForImage(tile);
						if(d.rows() > 0) {
							dsift_rows += d.rows();
							denseSiftDescriptors.add(d); // better have the same number of cols!
						}
					default://do nothing, will fail if we get here
				}
				mp.updateSignature(id, label, sig);
			}
		}

		System.out.println("building SIFT signatures");
		buildSiftSignatures(mp, keys,siftDescriptors,sift_rows,buffer, false); // SIFT
		System.out.println("building denseSIFT signatures");
		buildSiftSignatures(mp, keys,denseSiftDescriptors,dsift_rows,buffer, true); // denseSIFT

		return mp;
	}
	
	// for SIFT and denseSIFT
	public static void buildSiftSignatures(SignatureMap mp, List<TileKey> keys, List<Mat> siftDescriptors, int rows, DiskNiceTileBuffer buffer,
			boolean useDenseSift) {
		if(siftDescriptors.size() > 0) {
			// merge into a single matrix
			int cols = siftDescriptors.get(0).cols();
			Mat finalMatrix = Mat.zeros(rows,cols, siftDescriptors.get(0).type());
			//System.out.println("finalMatrix=("+rows+","+cols+")");
			int curr = 0;
			System.out.println("consolidating keypoints");
			for(int i = 0; i < siftDescriptors.size(); i++) {
				Mat d = siftDescriptors.get(i);
				int r = d.rows();
				d.copyTo(finalMatrix.submat(curr,curr+r,0,cols));
				curr += r;
			}
			// these clusters are our visual words
			// each center represents the center of a word
			System.out.println("running k-means with k="+SiftSignatureModel.defaultVocabSize);
			Mat centers = Signatures.getKmeansCenters(finalMatrix, SiftSignatureModel.defaultVocabSize);
			System.out.println("building KD-tree");
			KDTree<Integer> vocab = Signatures.buildKDTree(centers); // used to find nearest neighbor fast
			int vocabSize = centers.rows();
/*
			if(vocabSize < defaultVocabSize) {
				for(int i = 0; i < roi.size(); i++) {
					TileKey rkey = roi.get(i);
					System.out.print(rkey.buildTileStringForFile()+ "-");
					System.out.print(all_descriptors.get(i).rows()+" ");
				}
				System.out.println();
			}
*/

			// now go back through and build the final histograms for each ROI
			System.out.println("building final histograms");
			if(useDenseSift) {
				for(int i = 0; i < siftDescriptors.size(); i++) {
					Mat d = siftDescriptors.get(i);
					TileKey id = keys.get(i);
					// build a dense sift signature instead
					double[] sig = Signatures.buildDenseSiftSignature(d, vocab, vocabSize);
					mp.updateSignature(id, Model.SIFT, sig);
				}
			} else {
				for(int i = 0; i < siftDescriptors.size(); i++) {
					Mat d = siftDescriptors.get(i);
					TileKey id = keys.get(i);
					double[] sig = Signatures.buildSiftSignature(d, vocab, vocabSize);
					mp.updateSignature(id, Model.SIFT, sig);
				}
			}
		}
	}
	
}
