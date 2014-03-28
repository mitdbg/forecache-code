package backend.prefetch.similarity;

import java.util.List;

import utils.DBInterface;
import utils.UserRequest;

public class TrainModels {
	public static String defaulttaskname = "task1";
	
	/*
	// train all relevant models
	public static void train(int[] user_ids, Model[] models) {
		// get user traces
		// run training functions for each model
	}
	*/
	
	// for backend to easily train directional markov model given specific user traces
	public static void TrainMarkovDirectionalModel(int[] user_ids, String taskname, MarkovDirectionalModel model) {
		// get user traces
		for(int i = 0; i < user_ids.length; i++) {
			int user_id = user_ids[i];
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id, taskname);
			model.train(trace);
		}
		model.learnProbabilities();
	}
	
	public static void TrainMarkovDirectionalModel(int[] user_ids, MarkovDirectionalModel model) {
		TrainMarkovDirectionalModel(user_ids,defaulttaskname,model);
	}
}
