package abstraction.prediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import abstraction.enums.Model;
import abstraction.prediction.directional.HotspotDirectionalModel;
import abstraction.prediction.directional.MomentumDirectionalModel;
import abstraction.prediction.directional.NGramDirectionalModel;
import abstraction.prediction.directional.RandomDirectionalModel;
import abstraction.prediction.signature.DenseSiftSignatureModel;
import abstraction.prediction.signature.FilteredHistogramSignatureModel;
import abstraction.prediction.signature.HistogramSignatureModel;
import abstraction.prediction.signature.NormalSignatureModel;
import abstraction.prediction.signature.SiftSignatureModel;
import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.structures.SessionMetadata;
import abstraction.structures.SessionMetadata.ModelSetup;

public class PredictionEngine {
	public SvmPhaseClassifier.SvmWrapper phaseClassifier;

	// General Model variables
	public int[] allocatedStorage; // storage allocated per model

	public Model[] modellabels = null;
	public int[] historylengths = null;

	// global model objects	
	public BasicModel[] models;
	public List<PredictionTask> modelTasks = null;
	
	ExecutorService threadPool;
	int poolsize = 2;

	public PredictionEngine() {
		phaseClassifier = SvmPhaseClassifier.buildSvmPhaseClassifier();
	}
	
	public synchronized void init() {
		this.threadPool = Executors.newFixedThreadPool(poolsize);
	}
	
	public synchronized void shutdown() {
		this.threadPool.shutdown();
	}
	
	public void update_allocations(SessionMetadata md) {
		AnalysisPhase phase = phaseClassifier.predictAnalysisPhase(md.history.getHistoryTrace(2)); // only need last 2 tiles requests
		AllocationStrategy currentStrategy = md.allocationStrategyMap.get(phase);
		allocatedStorage = currentStrategy.allocations;
	}
	
	// returns a set of unique tile keys
	public synchronized List<NewTileKey> getPredictions(SessionMetadata md,
			DefinedTileView dtv, int maxDist) {
		List<NewTileKey> values = new ArrayList<NewTileKey>();
		// get the current allocation strategy
		update_allocations(md);
		
		// run the recommendation models
		// note: futures are returned in the same order as the original PredictionTask objects
		List<Future<List<NewTileKey>>> results = getPredictionsHelper(md,dtv, maxDist);
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
						values.add(prediction);
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
		return values;
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
	
	public void trainModels(int[] user_ids, String taskname) {
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
		models = null;
		modelTasks = null;
		//defaultpredictions = Integer.parseInt(predictions);
		//System.out.println("predictions: "+defaultpredictions);
	}
	
	public void reset(String taskname, int[] users, String[] modelstrs, int baselen) {
		modelTasks = null;
		
		setupModels();
		ModelSetup ms = SessionMetadata.parseModelStrings(modelstrs, baselen);
		modellabels = ms.modellabels;
		historylengths = ms.historylengths;
		trainModels(users,null);
	}
	
	public void reset(String taskname, String[] userstrs, String[] modelstrs, int baselen) throws Exception {
		modelTasks = null;
		
		setupModels();
		ModelSetup ms = SessionMetadata.parseModelStrings(modelstrs, baselen);
		modellabels = ms.modellabels;
		historylengths = ms.historylengths;
		trainModels(SessionMetadata.parseUserStrings(userstrs),taskname);
	}
	
	/********************* Helper Functions ************************/
	protected List<Future<List<NewTileKey>>> getPredictionsHelper(SessionMetadata md,
			DefinedTileView dtv, int maxDist) {
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
			// run the predictors, wait for results
			return this.threadPool.invokeAll(modelTasks);
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
