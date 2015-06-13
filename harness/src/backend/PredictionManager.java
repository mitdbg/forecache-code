package backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.TileInterface;
import backend.memory.NiceTileLruBuffer;
import backend.prediction.BasicModel;
import backend.prediction.TestSVM;
import backend.prediction.TileHistoryQueue;
import backend.prediction.TrainModels;
import backend.prediction.directional.HotspotDirectionalModel;
import backend.prediction.directional.MomentumDirectionalModel;
import backend.prediction.directional.NGramDirectionalModel;
import backend.prediction.directional.RandomDirectionalModel;
import backend.prediction.signature.DenseSiftSignatureModel;
import backend.prediction.signature.FilteredHistogramSignatureModel;
import backend.prediction.signature.HistogramSignatureModel;
import backend.prediction.signature.NormalSignatureModel;
import backend.prediction.signature.SiftSignatureModel;
import backend.util.Model;
import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.SignatureMap;
import backend.util.TileKey;

public class PredictionManager {
	public NiceTileBuffer buf;
	public NiceTileLruBuffer lmbuf;
	public DiskNiceTileBuffer diskbuf;
	public TileInterface dbapi;
	
	public int histmax = 10;
	public TileHistoryQueue hist;
	
	public SignatureMap sigMap;
	
	public TestSVM.SvmWrapper pclas;
	public boolean usePclas = false;
	
	//accuracy
	public int total_requests = 0;
	public int cache_hits = 0;
	public List<String> hitslist = new ArrayList<String>();
	
	// General Model variables
	public int deflmbuflen = 0; // default is don't use lru cache
	public int defaultpredictions = 0;
	public int defaulthistorylength = 4;
	public int defaultport = 8080;
	public int[] allocatedStorage; // storage per model
	public int totalStorage = 0;
	public int defaultstorage = 0; // default storage per model
	public int neighborhood = 1; // default neighborhood from which to pick candidates
	
	public Model[] modellabels = {Model.MOMENTUM};
	public int[] historylengths = {defaulthistorylength};
	public String taskname = "task1";
	public int[] user_ids = {28};
	
	// global model objects	
	public BasicModel[] all_models;
	
	//server
	public RunnableFuture<?> predictor = null;
	
	public PredictionManager() {
		pclas = TestSVM.buildSvmPhaseClassifier();
	}
	
	public synchronized void runPredictor(ExecutorService executorService) {
		predictor = new FutureTask<Object>(new PredictionTask(),null);
		executorService.submit(predictor);
	}
	
	public synchronized boolean isReady() {
		return (predictor == null) || (predictor.isDone());
	}
	
	public void updateAccuracy(TileKey id) {
		boolean found = buf.peek(id);
		if(found) {
			cache_hits++;
			hitslist.add("hit");
		} else {
			hitslist.add("miss");
		}
		total_requests++;
	}
	
	public double getAccuracy() {
		return 1.0 * cache_hits / total_requests;
	}
	
	public String getFullAccuracy() {
		if(hitslist.size() == 0) {
			return "[]";
		}
		String res = hitslist.get(0);
		for(int i = 1; i < hitslist.size(); i++) {
			res = res + ","+hitslist.get(i);
		}
		return res;
	}
	
	public void setupModels() {
		all_models = new BasicModel[modellabels.length];
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			switch(label) {
				case NGRAM: all_models[i] = new NGramDirectionalModel(hist,buf,diskbuf,dbapi,historylengths[i]);
				break;
				case RANDOM: all_models[i] = new RandomDirectionalModel(hist,buf,diskbuf,dbapi,historylengths[i]);
				break;
				case HOTSPOT: all_models[i] = new HotspotDirectionalModel(hist,buf,diskbuf,dbapi,historylengths[i],HotspotDirectionalModel.defaulthotspotlen);
				break;
				case MOMENTUM: all_models[i] = new MomentumDirectionalModel(hist,buf,diskbuf,dbapi,historylengths[i]);
				break;
				case NORMAL: all_models[i] = new NormalSignatureModel(hist,buf,diskbuf,dbapi,historylengths[i],sigMap);
				break;
				case HISTOGRAM: all_models[i] = new HistogramSignatureModel(hist,buf,diskbuf,dbapi,historylengths[i],sigMap);
				break;
				case FHISTOGRAM: all_models[i] = new FilteredHistogramSignatureModel(hist,buf,diskbuf,dbapi,historylengths[i],sigMap);
				break;
				case SIFT: all_models[i] = new SiftSignatureModel(hist,buf,diskbuf,dbapi,historylengths[i],sigMap);
				break;
				case DSIFT: all_models[i] = new DenseSiftSignatureModel(hist,buf,diskbuf,dbapi,historylengths[i],sigMap);
				default://do nothing, will fail if we get here
			}
		}
	}
	
	public void trainModels() {
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			BasicModel mod = all_models[i];
			switch(label) {
				case NGRAM: TrainModels.TrainNGramDirectionalModel(user_ids, taskname, (NGramDirectionalModel) mod);
				break;
				case HOTSPOT: TrainModels.TrainHotspotDirectionalModel(user_ids, taskname, (HotspotDirectionalModel) mod);
				break;
				default://do nothing
			}
		}
	}
	
	public void doPredictions() {
		if(modellabels.length == 0) return;
		
		//update allocations amongst predictors
		update_allocations();
		
		// running list of tiles to insert for prediction
		Map<TileKey,Boolean> toInsert = new HashMap<TileKey,Boolean>();
		
		// get the current list of candidates
		//System.out.println("neighborhood: "+neighborhood);
		List<TileKey> candidates = all_models[0].getCandidates(neighborhood);
		
		for(int m = 0; m < modellabels.length; m++) { // for each model
			//Model label = modellabels[m];
			BasicModel mod = all_models[m];
			//boolean doShift = shiftby4 && (Model.SIFT == label);
			List<TileKey> orderedCandidates = mod.orderCandidates(candidates);
			int count = 0;
			for(int i = 0; i < orderedCandidates.size(); i++) {
				//if(doShift && (i < 4)) continue;
				if(count == allocatedStorage[m]) break;
				TileKey key = orderedCandidates.get(i);
				if(!toInsert.containsKey(key) // not already slated to be inserted
						&& !lmbuf.peek(key)) { // not in lru cache already
					toInsert.put(key, true);
					count++;
				}
			}

			//System.out.print("predicted ordering for model "+label+": ");
			//for(int i = 0; i < orderedCandidates.size(); i++) {
			//	System.out.print(orderedCandidates.get(i)+" ");
			//}
			//System.out.println();
		}
		/*
		System.out.print("predictions:");
		for(TileKey k : toInsert.keySet()) {
			System.out.print(k+" ");
		}
		System.out.println();
		System.out.println();
		
		Set<TileKey> oldKeys = new HashSet<TileKey>();
		for(TileKey k : membuf.getAllTileKeys()) {
			oldKeys.add(k);
		}
		*/
		//for(TileKey old : oldKeys) {
		//	if (!toInsert.containsKey(old)) {
		//		membuf.removeTile(old);
		//	}
		//}
		List<TileKey> insertList = new ArrayList<TileKey>();
		insertList.addAll(toInsert.keySet());
		insertPredictions(insertList);
	}
	
	public void insertPredictions(List<TileKey> predictions) {
		//System.out.print("predictions to insert:");
		//for(TileKey key : predictions) {
		//	System.out.print(key+" ");
		//}
		//System.out.println();
		if(predictions != null) {
			buf.clear();
			for(TileKey key: predictions) { // insert the predictions into the cache
				if(!buf.peek(key)) { // not in memory
					NiceTile tile = diskbuf.getTile(key);
					if(tile == null) { // not on disk
						// get from database
						tile = dbapi.getNiceTile(key);
						//insert in disk cache
						diskbuf.insertTile(tile);
					} else { // found it on disk
						// update timestamp
						diskbuf.touchTile(key);
					}
					//insert in buf
					buf.insertTile(tile);
				} else { // found it in memory
					// update timestamp
					buf.touchTile(key);
				}
			}
		}
		//System.out.print("tiles in cache:");
		//for(TileKey key : membuf.getAllTileKeys()) {
		//	System.out.print(key+" ");
		//}
		//System.out.println();
	}
	
	public void update_users(String[] userstrs) {
		int[] argusers = new int[userstrs.length];
		for(int i = 0; i < userstrs.length; i++) {
			System.out.println("userstrs["+i+"] = '"+userstrs[i]+"'");
			argusers[i] = Integer.parseInt(userstrs[i]);
		}
		user_ids = argusers;
	}
	
	public void update_model_labels(String[] modelstrs) {
		modellabels = new Model[modelstrs.length];
		historylengths = new int[modelstrs.length];
		for(int i = 0; i < modelstrs.length; i++) {
			System.out.println("modelstrs["+i+"] = '"+modelstrs[i]+"'");
			historylengths[i] = defaulthistorylength;
			if(modelstrs[i].contains("ngram")) {
				modellabels[i] = Model.NGRAM;
				historylengths[i] = Integer.parseInt(modelstrs[i].substring(5));
			} else if(modelstrs[i].equals("random")) {
				modellabels[i] = Model.RANDOM;
			} else if(modelstrs[i].contains("hotspot")) {
				modellabels[i] = Model.HOTSPOT;
				if(modelstrs[i].length() > 7) {
					historylengths[i] = Integer.parseInt(modelstrs[i].substring(7));
				}
			} else if(modelstrs[i].contains("momentum")) {
				modellabels[i] = Model.MOMENTUM;
				if(modelstrs[i].length() > 8) {
					historylengths[i] = Integer.parseInt(modelstrs[i].substring(8));
				}
			} else if(modelstrs[i].equals("normal")) {
				modellabels[i] = Model.NORMAL;
			} else if(modelstrs[i].equals("histogram")) {
				modellabels[i] = Model.HISTOGRAM;
			} else if(modelstrs[i].equals("fhistogram")) {
				modellabels[i] = Model.FHISTOGRAM;
			} else if(modelstrs[i].equals("sift")) {
				modellabels[i] = Model.SIFT;
			} else if (modelstrs[i].equals("dsift")) {
				modellabels[i] = Model.DSIFT;
			}
		}
	}
	
	public void update_allocations() {
		if(usePclas) {
			buf.clear();
			buf.setStorageMax(totalStorage);
			
			String phase = pclas.predictLabel(hist.getHistoryTrace(2)); // only need last 2 tile requests
			allocatedStorage = new int[allocatedStorage.length]; // clear previous allocations
			int idx = -1;
			//System.out.println("predicted phase: "+phase);
			if (phase.equals("Sensemaking")) {
				idx = indexOf(modellabels,Model.SIFT);
				if(idx >= 0) allocatedStorage[idx] = totalStorage;
			} else if(totalStorage > 4){
					idx = indexOf(modellabels,Model.NGRAM);
					if(idx >= 0) allocatedStorage[idx] = 4;
					idx = indexOf(modellabels,Model.SIFT);
					if(idx >= 0) allocatedStorage[idx] = totalStorage-4;
			} else {
				idx = indexOf(modellabels,Model.NGRAM);
				if(idx >= 0) allocatedStorage[idx] = totalStorage;
			}
		}
	}
	
	public int indexOf(Model[] mll, Model x) {
		for(int i = 0; i < mll.length; i++) {
			if(mll[i] == x) return i;
		}
		return -1;
	}
	
	public void update_allocations_from_string(String[] allocations) {
		int required = 0;
		allocatedStorage = new int[allocations.length];
		for(int i = 0; i < allocations.length; i++) {
			//System.out.println("allocations["+i+"] = '"+allocations[i]+"'");
			allocatedStorage[i] = Integer.parseInt(allocations[i]);
			required += allocatedStorage[i];
		}
		buf.clear();
		buf.setStorageMax(required);
		totalStorage = required;
	}
	
	public void clear() {
		update_users(new String[]{});
		update_model_labels(new String[]{});
		update_allocations_from_string(new String[]{});
		all_models = null;
		usePclas = false;
		//defaultpredictions = Integer.parseInt(predictions);
		//System.out.println("predictions: "+defaultpredictions);
		
		//reset accuracy
		cache_hits = 0;
		total_requests = 0;
		hitslist.clear();
		
		// reinitialize caches and user history
		totalStorage = 0;
		buf.clear();
		lmbuf.clear();
		hist.clear();
	}
	
	public void reset(String[] userstrs, String[] modelstrs, String[] predictions,boolean usePhases) throws Exception {
		update_users(userstrs);
		update_model_labels(modelstrs);
		update_allocations_from_string(predictions);
		usePclas = usePhases;
		//defaultpredictions = Integer.parseInt(predictions);
		//System.out.println("predictions: "+defaultpredictions);
		
		//reset accuracy
		cache_hits = 0;
		total_requests = 0;
		hitslist.clear();
		
		// reinitialize caches and user history
		buf.clear();
		lmbuf.clear();
		hist.clear();
		
		setupModels();
		trainModels();
	}
	
	/*this thread is used to populate the main memory cache. It is triggered after each request*/
	public class PredictionTask implements Runnable {
		public synchronized void run() {
			doPredictions();
		}
	}
}
