package abstraction.prediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

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
import abstraction.util.Model;
import abstraction.util.NewTileKey;

public class PredictionEngine {
	public int histmax = 10;
	public TileHistoryQueue hist;

	public TestSVM.SvmWrapper phaseClassifier;
	public boolean use_phase_classifier = false;

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

	public PredictionEngine() {
		phaseClassifier = TestSVM.buildSvmPhaseClassifier();
	}

	public synchronized void cancelPredictorJob() throws Exception {
		if(predictor !=null) {
			// try to cancel the job
			for(int i = 0; (i < 10) && !predictor.isDone(); i++) {
				predictor.cancel(true);
			}
			if(!predictor.isDone()) {
				throw new Exception("Could not cancel predictor job!!!");
			}
		}
	}

	public synchronized void runPredictor(ExecutorService executorService) {
		predictor = new FutureTask<Object>(new PredictionTask(),null);
		executorService.submit(predictor);
	}

	public synchronized boolean isReady() {
		return (predictor == null) || (predictor.isDone());
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

	public String[] getFullAccuracyRaw() {
		return hitslist.toArray(new String[hitslist.size()]);
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
	
	public void clear() {
		//update_users(new String[]{});
		//update_model_labels(new String[]{});
		//update_allocations_from_string(new String[]{});
		all_models = null;
		use_phase_classifier = false;
		neighborhood = 1;
		predictor = null;
		//defaultpredictions = Integer.parseInt(predictions);
		//System.out.println("predictions: "+defaultpredictions);
		
		//reset accuracy
		cache_hits = 0;
		total_requests = 0;
		hitslist.clear();
	}
	
	public void reset(int[] users, String[] modelstrs, int[] allocations,
			int neighborhood, boolean usePhase) {
		user_ids = users;
		//update_model_labels(modelstrs);
		//update_allocations2(allocations);
		this.neighborhood = neighborhood;
		this.use_phase_classifier = usePhase;
		predictor = null;
		
		//reset accuracy
		cache_hits = 0;
		total_requests = 0;
		hitslist.clear();
		
		setupModels();
		trainModels();
	}
	
	public void reset(String[] userstrs, String[] modelstrs, String[] predictions,
			String nstr,boolean usePhases) throws Exception {
		//update_users(userstrs);
		//update_model_labels(modelstrs);
		//update_allocations_from_string(predictions);
		use_phase_classifier = usePhases;
		neighborhood = Integer.parseInt(nstr);
		predictor = null;
		//System.out.println("new neighborhood variable:"+neighborhood);
		
		//reset accuracy
		cache_hits = 0;
		total_requests = 0;
		hitslist.clear();
		
		setupModels();
		trainModels();
	}

	/*this thread is used to populate the main memory cache. It is triggered after each request*/
	public class PredictionTask implements Runnable {
		public synchronized void run() {
			System.out.println();
		}
	}
}
