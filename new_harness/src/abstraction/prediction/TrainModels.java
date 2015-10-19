package abstraction.prediction;

import java.util.List;


import abstraction.prediction.directional.HotspotDirectionalModel;
import abstraction.prediction.directional.MarkovChainDirectionalModel;
import abstraction.prediction.directional.NGramDirectionalModel;
import abstraction.structures.UserRequest;
import abstraction.util.DBInterface;

public class TrainModels {
	public static String defaulttaskname = "task1";
	
	// for backend to easily train directional markov model given specific user traces
	public static void TrainMarkovChainDirectionalModel(int[] user_ids, String taskname, MarkovChainDirectionalModel model) {
		// get user traces
		for(int i = 0; i < user_ids.length; i++) {
			int user_id = user_ids[i];
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id, taskname);
			model.train(trace);
		}
		model.finishTraining();
	}
	
	// for backend to easily train directional markov model given specific user traces
	public static void TrainNGramDirectionalModel(int[] user_ids, String taskname, NGramDirectionalModel model) {
		// get user traces
		for(int i = 0; i < user_ids.length; i++) {
			int user_id = user_ids[i];
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id, taskname);
			model.train(trace);
		}
		model.finishTraining();
	}
	
	public static void TrainHotspotDirectionalModel(int[] user_ids, String taskname, HotspotDirectionalModel model) {
		// get user traces
		for(int i = 0; i < user_ids.length; i++) {
			int user_id = user_ids[i];
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id, taskname);
			model.train(trace);
		}
		model.finishTraining();
	}
	
	public static void TrainHotspotDirectionalModel(int[] user_ids,HotspotDirectionalModel model) {
		TrainHotspotDirectionalModel(user_ids,defaulttaskname,model);
	}

}
