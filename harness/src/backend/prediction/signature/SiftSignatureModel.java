package backend.prediction.signature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryTileBuffer;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.prediction.directional.MarkovDirectionalModel;
import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.Signatures;
import backend.util.Tile;
import backend.util.TileKey;

public class SiftSignatureModel {
	private TileHistoryQueue history = null;
	private ParamsMap paramsMap; // for checking if predictions are actual tiles
	private MemoryTileBuffer membuf;
	private DiskTileBuffer diskbuf;
	private ScidbTileInterface scidbapi;
	public static final double defaultprob = .00000000001;
	
	public static int defaultVocabSize = 5;
	protected List<double[]> roiHistograms = null;
	protected boolean haveRealRoi = false;
	KDTree<Integer> vocab = null;
	protected Map<TileKey,double[]> histograms;

	public SiftSignatureModel(TileHistoryQueue ref, MemoryTileBuffer membuf, DiskTileBuffer diskbuf,ScidbTileInterface api) {
		this.history = ref; // reference to (syncrhonized) global history object
		this.membuf = membuf;
		this.diskbuf = diskbuf;
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		this.scidbapi = api;
		this.histograms = new HashMap<TileKey,double[]>();
	}
	
	// gets ordering of directions by confidence and returns topk viable options
	public List<TileKey> predictTiles(int topk) throws Exception {
		List<TileKey> myresult = new ArrayList<TileKey>();
		if(this.history == null) {
			return myresult;
		}
		// do we have access to the last request?
		List<UserRequest> htrace = history.getHistoryTrace(1);
		if(htrace.size() == 0) {
			return myresult;
		}
		if(history.newRoi()) { // if there is a new ROI, update!
			updateRoi(history.getLastRoi(),scidbapi);
			haveRealRoi = true;
		} else if (!haveRealRoi){
			updateRoi(history.getLastRoi(),scidbapi);
		}
		List<DirectionPrediction> order = predictOrder(htrace);
		UserRequest last = htrace.get(0);
		for(int i = 0; i < order.size(); i++) {
			DirectionPrediction dp = order.get(i);
			TileKey val = this.DirectionToTile(last, dp.d);
			if(val != null) {
				myresult.add(val);
				//System.out.println(val);
			}
		}
		//System.out.println("viable options: "+myresult.size());
		if(topk >= myresult.size()) { // truncate if list is too long
			topk = myresult.size() - 1;
		}
		myresult = myresult.subList(0, topk);
		return myresult;
	}
	
	public List<DirectionPrediction> predictOrder(List<UserRequest> htrace) throws Exception {
		UserRequest prev = htrace.get(htrace.size()-1);
		TileKey pkey = MarkovDirectionalModel.getKeyFromRequest(prev);
		Tile orig = getTile(pkey);
		List<DirectionPrediction> order = new ArrayList<DirectionPrediction>();
		if(orig == null) { // tile not on disk at least!?
			return order;
		}
		long start = System.currentTimeMillis();
		// for each direction, compute confidence
		for(Direction d : Direction.values()) {
			DirectionPrediction dp = new DirectionPrediction();
			TileKey ckey = this.DirectionToTile(prev, d);
			Tile candidate = null;
			if(ckey != null) {
				candidate = getTile(ckey);
			}
			
			dp.d = d;
			dp.confidence = 0.0;
			if(candidate != null) {
				double[] vocabhist = buildSignature(candidate.getTileKey());
				for(double[] roihist : roiHistograms) {
					dp.confidence += Signatures.chiSquaredDistance(vocabhist, roihist);
				}
			}
			if(dp.confidence < defaultprob) {
				dp.confidence = defaultprob;
			}
			order.add(dp);
		}
		Collections.sort(order); // smaller numbers are better here
		long end = System.currentTimeMillis();
		/*
		for(DirectionPrediction dp : order) {
			System.out.println(dp);
		}*/
		//System.out.println("time to predict order: "+(end-start)+"ms");
		return order;
	}
	
	public Tile getTile(TileKey key) {
		Tile t = membuf.getTile(key);
		if(t == null) {
			t = diskbuf.getTile(key);
			if(t == null) {
				t = this.scidbapi.getTile(key);
			}
		}
		return t;
	}
	
	public TileKey DirectionToTile(UserRequest prev, Direction d) {
		List<Integer> tile_id = UtilityFunctions.parseTileIdInteger(prev.tile_id);
		int x = tile_id.get(0);
		int y = tile_id.get(1);
		int zoom = prev.zoom;
		
		// if zooming in, update values
		if((d == Direction.IN1) || (d == Direction.IN2) || (d == Direction.IN3) || (d == Direction.IN0)) {
			zoom++;
			x *= 2;
			y *=2;
			tile_id.set(0,x);
			tile_id.set(1,y);
		}
		
		switch(d) {
		case UP:
			tile_id.set(1,y+1);
			break;
		case DOWN:
			tile_id.set(1,y-1);
			break;
		case LEFT:
			tile_id.set(0,x-1);
			break;
		case RIGHT:
			tile_id.set(0,x+1);
			break;
		case OUT:
			zoom -= 1;
			x /= 2;
			y /= 2;
			tile_id.set(0,x);
			tile_id.set(1,y);
			break;
		case IN0: // handled above
			break;
		case IN1:
			tile_id.set(1,y+1);
			break;
		case IN3:
			tile_id.set(0,x+1);
			break;
		case IN2:
			tile_id.set(0,x+1);
			tile_id.set(1,y+1);
			break;
		}
		TileKey key = new TileKey(tile_id,zoom);
		//System.out.println("last access: ("+prev.tile_id+", "+prev.zoom+")");
		Map<Integer,Params> map1 = this.paramsMap.get(key.buildTileString());
		if(map1 == null) {
			return null;
		}
		Params p = map1.get(key.getZoom());
		if(p == null) {
			return null;
		}
		//System.out.println("recommendation: "+key);
		return key;
	}
	
	public void updateRoi(List<TileKey> roi, ScidbTileInterface scidbapi) {
		List<Mat> all_descriptors = new ArrayList<Mat>();
		int rows = 0;
		int cols = 0;
		for(TileKey id : roi) { // for each tile in the ROI
			Mat d = Signatures.getSiftDescriptorsForImage(id, scidbapi);
			rows += d.rows();
			all_descriptors.add(d); // better have the same number of cols!
		}
		// merge into a single matrix
		if(all_descriptors.size() > 0) {
			cols = all_descriptors.get(0).cols();
			Mat finalMatrix = Mat.zeros(rows,cols, all_descriptors.get(0).type());
			System.out.println("finalMatrix=("+rows+","+cols+")");
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
			vocab = Signatures.buildKDTree(centers); // used to find nearest neighbor fast

			// now go back through and build histograms for each ROI
			roiHistograms = new ArrayList<double[]>();
			for(int i = 0; i < all_descriptors.size(); i++) {
				Mat d = all_descriptors.get(i);
				TileKey id = roi.get(i);
				double[] signature = buildSignatureFromMat(d);
				roiHistograms.add(signature);
				histograms.put(id, signature); // save for later use
			}
		}
	}
	
	//TODO: override these to implement a new image signature!
	public double[] buildSignatureFromMat(Mat d) {
		return Signatures.buildSiftSignature(d, vocab, defaultVocabSize);
	}
	
	public double[] buildSignatureFromKey(TileKey id) {
		return Signatures.buildSiftSignature(id, vocab, defaultVocabSize);
	}
	
	public double[] buildSignature(TileKey id) {
		double[] signature = histograms.get(id);
		if(signature == null) {
			signature = buildSignatureFromKey(id);
			histograms.put(id, signature);
		}
		return signature;
	}
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		String idstr = "[0, 0]";
		int zoom = 1;
		List<Integer> tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		TileKey id = new TileKey(tile_id,zoom);
		int vocabSize = defaultVocabSize;
		ScidbTileInterface scidbapi = new ScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		Mat descriptors = Signatures.getSiftDescriptorsForImage(id, scidbapi);
		Mat centers = Signatures.getKmeansCenters(descriptors, vocabSize);
		System.out.println("centers dimensions: "+centers.dims()+", ("+centers.rows()+","+centers.cols()+")");
		//for(int i = 0; i < centers.cols(); i++) {
		//	System.out.print(centers.get(0, i)[0]+" ");
		//	System.out.println();
		//}
		
		KDTree<Integer> vocab = Signatures.buildKDTree(centers);
		double[] hist = Signatures.buildSiftSignature(descriptors, vocab, defaultVocabSize);
		for(int i = 0; i < hist.length; i++) {
			System.out.print(" "+hist[i]);
		}
		System.out.println();
	}
}
