package frontend;

import java.util.ArrayList;
import java.util.List;

import utils.DBInterface;
import utils.UserRequest;

public class TraceMovementAnalysis {
	public static void crossValidation(String taskname, String[] models, int predictions) throws Exception {
		List<Integer> users = DBInterface.getUsers();
		List<Integer> finalusers = new ArrayList<Integer>();
		
		for(int u = 0; u < users.size(); u++) {
			if(DBInterface.checkTask(users.get(u),taskname)) {
				finalusers.add(users.get(u));
			}
		}
		
		if(finalusers.size() == 0) {
			System.out.println("nothing to test! exiting...");
			return;
		} else if (finalusers.size() == 1) {
			System.out.println("only 1 user! exiting...");
			return;
		}
		
		double overall_accuracy = 0;
		// u1 = position of user we are testing
		for(int u1 = 0; u1 < finalusers.size(); u1++) {
			// get training list
			int[] trainlist = new int[finalusers.size() - 1];
			//System.out.print("train list: ");
			int index = 0;
			for(int u2 = 0; u2 < finalusers.size(); u2++) {
				if(u2 != u1) {
					trainlist[index] = finalusers.get(u2);
					//System.out.print(trainlist[index]+" ");
					index++;
				}
			}

			//send requests
			int user_id = finalusers.get(u1);
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id,taskname);
			for(int r = 0; r < trace.size(); r++) {
				UserRequest ur = trace.get(r);
				int zoom = ur.zoom;
			}
		}
	}
	
	public static void dumpTraces() {
		
	}
}
