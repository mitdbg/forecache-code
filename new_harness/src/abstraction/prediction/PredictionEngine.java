package abstraction.prediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import backend.prediction.BasicModel;
import backend.prediction.TestSVM;
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
	public TestSVM.SvmWrapper phaseClassifier;
	public boolean use_phase_classifier = false;

	//accuracy
	public int total_requests = 0;
	public int cache_hits = 0;
	public List<String> hitslist = new ArrayList<String>();

	// General Model variables
	public int[] allocatedStorage; // storage allocated per model
	public int neighborhood = -1;

	public Model[] modellabels = null;
	public int[] historylengths = null;
	public String taskname = null;
	public int[] user_ids = null;

	// global model objects	
	public BasicModel[] models;
	public List<PredictionTask> modelTasks = null;

	public PredictionEngine() {
		phaseClassifier = TestSVM.buildSvmPhaseClassifier();
	}
	
	public void update_allocations(SessionMetadata md) {
		if(use_phase_classifier) {
			AnalysisPhase phase = phaseClassifier.predictAnalysisPhase(md.history.getHistoryTrace(2)); // only need last 2 tiles requests
			AllocationStrategy currentStrategy = md.allocationStrategyMap.get(phase);
			allocatedStorage = currentStrategy.allocations;
		}
	}
	
	// returns a set of unique tile keys
	public synchronized Map<NewTileKey,Boolean> getPredictions(ExecutorService executorService,
			SessionMetadata md, DefinedTileView dtv, int maxDist) {
		// get the current allocation strategy
		update_allocations(md);
		
		// run the recommendation models
		// note: futures are returned in the same order as the original PredictionTask objects
		List<Future<List<NewTileKey>>> results = getPredictionsHelper(executorService,
														md,dtv, maxDist);
		
		// consolidate the recommendations
		Map<NewTileKey,Boolean> predictions = new HashMap<NewTileKey,Boolean>();
		for(int i = 0; i < results.size(); i++) {
			Future<List<NewTileKey>> future = results.get(i);
			try {
				List<NewTileKey> fresults = future.get();
				int count = 0; // only add what has been allocated to this model
				for(int j = 0; (count < allocatedStorage[i]) && (j < fresults.size()); j++) {
					NewTileKey prediction = fresults.get(j);
					// was not in the map previously
					if(predictions.put(prediction,true) == null) {
						count++;
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return predictions;
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
		models = new BasicModel[modellabels.length];
		modelTasks = new ArrayList<PredictionTask>();
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			switch(label) {
				case NGRAM: models[i] = new NGramDirectionalModel(historylengths[i]);
				break;
				case RANDOM: models[i] = new RandomDirectionalModel(historylengths[i]);
				break;
				case HOTSPOT: models[i] = new HotspotDirectionalModel(historylengths[i],HotspotDirectionalModel.defaulthotspotlen);
				break;
				case MOMENTUM: models[i] = new MomentumDirectionalModel(historylengths[i]);
				break;
				case NORMAL: models[i] = new NormalSignatureModel(historylengths[i]);
				break;
				case HISTOGRAM: models[i] = new HistogramSignatureModel(historylengths[i]);
				break;
				case FHISTOGRAM: models[i] = new FilteredHistogramSignatureModel(historylengths[i]);
				break;
				case SIFT: models[i] = new SiftSignatureModel(historylengths[i]);
				break;
				case DSIFT: models[i] = new DenseSiftSignatureModel(historylengths[i]);
				default://do nothing, will fail if we get here
			}
			 PredictionTask newTask = new PredictionTask();
			 newTask.mid = i;
			 modelTasks.add(newTask);
		}
	}
	
	public void trainModels() {
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			BasicModel mod = models[i];
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
		models = null;
		modelTasks = null;
		use_phase_classifier = false;
		neighborhood = 1;
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
		modelTasks = null;
		
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
		modelTasks = null;
		//System.out.println("new neighborhood variable:"+neighborhood);
		
		//reset accuracy
		cache_hits = 0;
		total_requests = 0;
		hitslist.clear();
		
		setupModels();
		trainModels();
	}
	
	/********************* Helper Functions ************************/
	protected List<Future<List<NewTileKey>>> getPredictionsHelper(ExecutorService executorService,
			SessionMetadata md, DefinedTileView dtv, int maxDist) {
		// candidate tiles to rank
		List<NewTileKey> candidates = models[0].getCandidates(md, dtv, maxDist);
		
		// setup the supplementary data
		for(int i = 0; i < modelTasks.size(); i++) {
			PredictionTask task = modelTasks.get(i);
			task.md = md;
			task.dtv = dtv;
			task.candidates = candidates;
		}
		
		try {
			// run the predictors
			return executorService.invokeAll(modelTasks);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<Future<List<NewTileKey>>>();
	}
	
	protected int indexOf(Model[] mll, Model x) {
		for(int i = 0; i < mll.length; i++) {
			if(mll[i] == x) return i;
		}
		return -1;
	}
	
	/********************* Nested Classes ************************/
	/*this Task is used to make predictions. It is triggered after each request*/
	public class PredictionTask implements Callable<List<NewTileKey>> {
		public volatile int mid;
		public volatile SessionMetadata md;
		public volatile DefinedTileView dtv;
		public volatile List<NewTileKey> candidates;
		
		public synchronized List<NewTileKey> call() {
			BasicModel m = models[this.mid];
			return m.orderCandidates(md, dtv, candidates);
		}
	}
}
