package frontend;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import backend.util.Direction;
import backend.util.DirectionClass;
import backend.util.History;
import backend.util.ModelAccuracy;
import utils.DBInterface;
import utils.ExplorationPhase;
import utils.TraceMetadata;
import utils.UserRequest;
import utils.UtilityFunctions;

public class Client {
	public static String backend_host = "localhost";
	public static int backend_port = 8080;
	public static String backend_root = "gettile";
	//public static String [] tasknames = {"warmup", "task1", "task2", "task3"};
	public static String [] tasknames = {"task1", "task2", "task3"};
	
	public static void crossValidation1Model() throws Exception {
		//String[][] models = {{"random"},{"markov"},{"momentum"},{"hotspot"},{"normal"},{"histogram"},{"fhistogram"}};
		String[][] models = {{"normal"}};
		//int[] predictions = {1,3,5};
		int[] predictions = {1};
		for(String taskname : tasknames) {
			for(int i = 0; i < models.length; i++) {
				for(int predict = 0; predict < predictions.length; predict++) {
					crossValidation(taskname,models[i],predictions[predict], null);
				}
			}
		}
	}
	
	public static void crossValidationModelSpecific(int[] users, String[] tasknames, String[][] models, int[] predictions) throws Exception {
		List<Integer> testusers = new ArrayList<Integer>();
		for(int i = 0; i < users.length; i++) {
			testusers.add(users[i]);
		}
		
		for(String taskname : tasknames) {
			for(int predict = 0; predict < predictions.length; predict++) {
				ModelAccuracy[] ma = new ModelAccuracy[testusers.size()];
				for(int mai = 0; mai < ma.length; mai++) {
					ma[mai] = new ModelAccuracy();
				}
				for(int i = 0; i < models.length; i++) {
					crossValidation(taskname,models[i],testusers,new int[]{predictions[predict]}, ma);
				}
/*
				for(int mai = 0; mai < ma.length; mai++) {
					ma[mai].learnSimpleModelLabels();
					ma[mai].learnModelLabels();
					ma[mai].buildTrackRecords(4);
				}
*/
			}
		}
	}
	
	public static void crossValidationModelSpecific(int[] users, String[] tasknames, String[][] models, int[][] allocations) throws Exception {
		List<Integer> testusers = new ArrayList<Integer>();
		for(int i = 0; i < users.length; i++) {
			testusers.add(users[i]);
		}
		
		for(String taskname : tasknames) {
			for(int allc = 0; allc < allocations.length; allc++) {
				ModelAccuracy[] ma = new ModelAccuracy[testusers.size()];
				for(int mai = 0; mai < ma.length; mai++) {
					ma[mai] = new ModelAccuracy();
				}
				crossValidation(taskname,models[allc],testusers,allocations[allc], ma);
/*
				for(int mai = 0; mai < ma.length; mai++) {
					ma[mai].learnSimpleModelLabels();
					ma[mai].learnModelLabels();
					ma[mai].buildTrackRecords(4);
				}
*/
			}
		}
	}
	
	// test all users
	public static void crossValidation(String taskname, String[] models, int predictions, ModelAccuracy[] ma) throws Exception {
		List<Integer> testusers = DBInterface.getUsers();
		crossValidation(taskname,models,testusers,new int[]{predictions}, ma);

	}
	
	// only test specific users, but train on all users
	public static void crossValidation(String taskname, String[] models, List<Integer> testusers, int[] predictions,
			ModelAccuracy[] ma) throws Exception {
		List<Integer> users = DBInterface.getUsers();
		List<Integer> finalusers = new ArrayList<Integer>();
		
		for(int u = 0; u < users.size(); u++) {
			if(DBInterface.checkTask(users.get(u),taskname)) {
				finalusers.add(users.get(u));
			}
		}
		
		double overall_accuracy = 0;
		// u1 = position of user we are testing
		for(int u1 = 0; u1 < testusers.size(); u1++) {
			// get training list
			int[] trainlist = new int[finalusers.size() - 1];
			//System.out.print("train list: ");
			int index = 0;
			for(int u2 = 0; u2 < finalusers.size(); u2++) {
				if(!finalusers.get(u2).equals(testusers.get(u1))) {
					trainlist[index] = finalusers.get(u2);
					//System.out.print(trainlist[index]+" ");
					index++;
				}
			}
			//System.out.println();
			//System.out.print("train list: ");
			//UtilityFunctions.printIntArray(trainlist);
			// setup test case on backend
			sendReset(trainlist,models,predictions);
			
			//send requests
			int user_id = testusers.get(u1);
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id,taskname);
			long start = System.currentTimeMillis();
			for(int r = 0; r < trace.size(); r++) {
				UserRequest ur = trace.get(r);
				String tile_id = ur.tile_id;
				String tile_hash = ur.tile_hash;
				int zoom = ur.zoom;
				//System.out.println("tile id: '" +tile_id+ "'");
				sendRequest(tile_id,zoom,tile_hash);
			}
			long end = System.currentTimeMillis();
			System.out.println("duration: "+(1.0*(end-start)/1000)+" secs");
			//get accuracy for this user
			double accuracy = getAccuracy();
			String[] fullAccuracy = getFullAccuracy();
			if(ma != null) {
				// assumes this is only for individual models, and not combinations
				ma[u1].addModel(UtilityFunctions.getModelFromString(models[0]), fullAccuracy);
			}
			
			/*
			//original printed content
			System.out.print(user_id+"\t");
			System.out.print(taskname+"\t");
			UtilityFunctions.printStringArray(models);
			System.out.print("\t");
			System.out.print(predictions+"\t");
			UtilityFunctions.printStringArray(fullAccuracy);
			overall_accuracy += accuracy;
			System.out.print("\t");
			System.out.println(accuracy);
			*/
			
			// new printed content
			
			TraceMetadata metadata = RequestLabeler.getLabels(trace);
			List<DirectionClass> dirs = metadata.directionClasses;
			List<ExplorationPhase> phases = metadata.explorationPhases;
			for(int i = 0; i < trace.size(); i++) {
				UserRequest request = trace.get(i);
				int[] id = UtilityFunctions.parseTileIdInteger(request.tile_id);
				System.out.print(user_id+"\t"+taskname+"\t");
				UtilityFunctions.printStringArray(models);
				System.out.print("\t");
				UtilityFunctions.printIntArray(predictions);
				System.out.print("\t");
				System.out.println(request.zoom+"\t"+id[0]+"\t"+id[1]+"\t"+dirs.get(i)+"\t"+phases.get(i)+
						"\t"+fullAccuracy[i]);
				//System.out.println("\t"+predictions+"\t"+request.zoom+"\t"+id[0]+"\t"+id[1]+"\t"+dirs.get(i)+"\t"+phases.get(i)+
				//		"\t"+fullAccuracy[i]);
			}
		}
		overall_accuracy /= testusers.size();
		//System.out.println("overall\t"+overall_accuracy);
	}

	public static void getTracesForAllUsers() throws Exception {
		List<Integer> users = DBInterface.getUsers();
		for(int u = 0; u < users.size(); u++) {
			int user_id = users.get(u);
			for(int t = 0; t < tasknames.length; t++) {
				String taskname = tasknames[t];
				if(DBInterface.checkTask(user_id,taskname)) {
					System.out.println("user '" + user_id + "' completed task '" + taskname + "'");
					List<UserRequest> trace = DBInterface.getHashedTraces(user_id,taskname);
					System.out.println("found trace of size " + trace.size() + " for task '" + taskname + "' and user '" + user_id + "'");
					for(int r = 0; r < trace.size(); r++) {
						UserRequest ur = trace.get(r);
						String tile_id = ur.tile_id;
						String tile_hash = ur.tile_hash;
						int zoom = ur.zoom;
						//System.out.println("tile id: '" +tile_id+ "'");
						sendRequest(tile_id,zoom,tile_hash);
						// do stuff here to analyze trace


					}
				}
			}
		}
	}
	
	public static void printTracesForSpecificUsers() {
		//System.out.println("userid\ttaskname\tpath-id\tsteps\tdirection\tin-count\tout-count\tpancount");
		System.out.println("userid\ttaskname\tzoom\ttile-x\ttile-y\tdirection\texploration-phase");
		List<Integer> user_ids = DBInterface.getUsersFromTraces();
		List<String> tasks = DBInterface.getTasksFromTraces();
		for(int u = 0; u < user_ids.size(); u++) {
			int user_id = user_ids.get(u);
			for(int t = 0; t < tasks.size(); t++) {
				String taskname = tasks.get(t);
				List<UserRequest> trace = DBInterface.getUserTraces(user_id,taskname);
				TraceMetadata metadata = RequestLabeler.getLabels(trace);
				List<DirectionClass> dirs = metadata.directionClasses;
				List<ExplorationPhase> phases = metadata.explorationPhases;
				if(trace.size() > 0) {
					for(int i = 0; i < trace.size(); i++) {
						UserRequest request = trace.get(i);
						int[] id = UtilityFunctions.parseTileIdInteger(request.tile_id);
						System.out.println(user_id+"\t"+taskname+"\t"+request.zoom+"\t"+id[0]+"\t"+id[1]+"\t"+dirs.get(i)+"\t"+phases.get(i));
					}
				}
				/*
				History history = new History(4);
				if(trace.size() > 0) {
					int path_id = 0;
					UserRequest prev = trace.get(0);
					int steps = 1;
					for(int r = 1; r < trace.size(); r++, steps++) {
						UserRequest next = trace.get(r);
						//System.out.println(prev);
						//System.out.println(next);
						Direction dir = UtilityFunctions.getDirection(prev, next);
						if(dir != null) {
							if(prev.zoom == 0 && prev.tile_id.equals("[0, 0]")) {
								path_id++;
								steps = 1;
							}
							Map<DirectionClass,Integer> dist = history.getClassDistribution();
							int incount = dist.containsKey(DirectionClass.IN) ? dist.get(DirectionClass.IN) : 0;
							int outcount = dist.containsKey(DirectionClass.OUT) ? dist.get(DirectionClass.OUT) : 0;
							int pancount = dist.containsKey(DirectionClass.PAN) ? dist.get(DirectionClass.PAN) : 0;
							System.out.print(user_id+"\t"+taskname+"\t"+path_id+"\t"+steps+"\t"+dir);
							// print the counts of zooms and pans for the last X steps
							System.out.print("\t"+incount);
							System.out.print("\t"+outcount);
							System.out.print("\t"+pancount);
							System.out.println();
							history.add(dir);
						} else {
							
						}
						prev = next;
					}
				}
				*/
			}
		}
	}

	public static void getTracesForSpecificUsers(int[] user_ids, String[] tasks, String[] models, int predictions) throws Exception {
		int[] train = new int[user_ids.length - 1];
		for(int u = 0; u < user_ids.length; u++) {
			int user_id = user_ids[u];
			int count = 0;
			for(int i = 0; i < user_ids.length; i++) {
				if(user_id != user_ids[i]) {
				train[count] = user_ids[i];
				count++;
				}
			}
			for(int t = 0; t < tasks.length; t++) {
				String taskname = tasks[t];
				List<UserRequest> trace = DBInterface.getHashedTraces(user_id,taskname);
				System.out.println("found trace of size " + trace.size() + " for task '" + taskname + "' and user '" + user_id + "'");
				long average = 0;
				sendReset(user_ids,models,new int[]{predictions});
				for(int r = 0; r < trace.size(); r++) {
					UserRequest ur = trace.get(r);
					String tile_id = ur.tile_id;
					String tile_hash = ur.tile_hash;
					int zoom = ur.zoom;
					//System.out.println("tile id: '" +tile_id+ "'");
					long start = System.currentTimeMillis();
					sendRequest(tile_id,zoom,tile_hash);
					long end = System.currentTimeMillis();
					System.out.println("time to recieve result: "+(end-start)+"ms");
					average += end - start;
				}
				System.out.println("accuracy: "+getAccuracy());
				System.out.print("full accuracy:");
				UtilityFunctions.printStringArray(getFullAccuracy());
				if(trace.size() > 0) {
					System.out.println("average time to recieve result: " + (average/trace.size())+"ms");
				}
				//}
			}
		}
	}
	
	// tell server what user ids and models to train on
		public static String[] getFullAccuracy() {
			String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
					+ "?fullaccuracy";
			URL geturl = null;
			HttpURLConnection connection = null;
			BufferedReader reader = null;
			StringBuffer sbuffer = new StringBuffer();
			String line;
			String result = null;
			try {
				geturl = new URL(urlstring);
			} catch (MalformedURLException e) {
				System.out.println("error occurred while retrieving url object for: '"+urlstring+"'");
				e.printStackTrace();
			}
			if(geturl == null) {
				return new String[0];
			}

			try {
				connection = (HttpURLConnection) geturl.openConnection();
			} catch (IOException e) {
				System.out.println("error occured while opening connection to url: '"+urlstring+"'");
				e.printStackTrace();
			}
			if(connection == null) {
				return new String[0];
			}

			try {
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				while((line = reader.readLine()) != null) {
					sbuffer.append(line);
				}
				reader.close();
				result = sbuffer.toString();
				return result.split(",");
			} catch (IOException e) {
				System.out.println("Error retrieving response from url: '"+urlstring+"'");
				e.printStackTrace();
			}

			if(connection != null) {
				connection.disconnect();
			}
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return new String[0];
		}
	
	// tell server what user ids and models to train on
	public static double getAccuracy() {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?accuracy";
		URL geturl = null;
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuffer sbuffer = new StringBuffer();
		String line;
		String result = null;
		try {
			geturl = new URL(urlstring);
		} catch (MalformedURLException e) {
			System.out.println("error occurred while retrieving url object for: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(geturl == null) {
			return -100000.0;
		}

		try {
			connection = (HttpURLConnection) geturl.openConnection();
		} catch (IOException e) {
			System.out.println("error occured while opening connection to url: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(connection == null) {
			return -100000.0;
		}

		try {
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while((line = reader.readLine()) != null) {
				sbuffer.append(line);
			}
			reader.close();
			result = sbuffer.toString();
			return Double.parseDouble(result);
		} catch (IOException e) {
			System.out.println("Error retrieving response from url: '"+urlstring+"'");
			e.printStackTrace();
		}

		if(connection != null) {
			connection.disconnect();
		}
		if(reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return -100000.0;
	}

	// tell server what user ids and models to train on
	public static boolean sendReset(int[] user_ids, String[] models, int[] predictions) {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?"+buildResetParams(user_ids,models, predictions);
		URL geturl = null;
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuffer sbuffer = new StringBuffer();
		String line;
		String result = null;
		try {
			geturl = new URL(urlstring);
		} catch (MalformedURLException e) {
			System.out.println("error occurred while retrieving url object for: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(geturl == null) {
			return false;
		}

		try {
			connection = (HttpURLConnection) geturl.openConnection();
		} catch (IOException e) {
			System.out.println("error occured while opening connection to url: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(connection == null) {
			return false;
		}

		try {
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while((line = reader.readLine()) != null) {
				sbuffer.append(line);
			}
			reader.close();
			result = sbuffer.toString();
			return result.equals("done");
		} catch (IOException e) {
			System.out.println("Error retrieving response from url: '"+urlstring+"'");
			e.printStackTrace();
		}

		if(connection != null) {
			connection.disconnect();
		}
		if(reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	public static void sendRequest(String tile_id, int zoom, String hashed_query) throws Exception {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?" + buildUrlParams(hashed_query, tile_id, zoom);
		URL geturl = null;
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuffer sbuffer = new StringBuffer();
		String line;
		String result = null;
		try {
			geturl = new URL(urlstring);
		} catch (MalformedURLException e) {
			System.out.println("error occurred while retrieving url object for: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(geturl == null) {
			return;
		}

		try {
			connection = (HttpURLConnection) geturl.openConnection();
		} catch (IOException e) {
			System.out.println("error occured while opening connection to url: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(connection == null) {
			return;
		}

		try {
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while((line = reader.readLine()) != null) {
				sbuffer.append(line);
			}
			reader.close();
			result = sbuffer.toString();
			//System.out.println("tile ("+tile_id+", "+zoom+") result length: " + result.length());
			if(result.equals("error")) {
				throw new Exception("serious error occurred on backend while retrieving tile");
			}
		} catch (IOException e) {
			System.out.println("Error retrieving response from url: '"+urlstring+"'");
			e.printStackTrace();
		}

		if(connection != null) {
			connection.disconnect();
		}
		if(reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public static String buildUrlParams(String hashed_query, String tile_id, int zoom) {
		String params = "";
		//params += "hashed_query="+hashed_query;
		params += "threshold=" + DBInterface.threshold;
		params += "&zoom=" + zoom;
		//List<Integer> tile_ints = DBInterface.parseTileIdInteger(tile_id);
		String stripped = tile_id.substring(1, tile_id.length() - 1);
		params += "&tile_id=" + UtilityFunctions.urlify(stripped);
		return params;
	}
	
	public static String buildResetParams(int[] user_ids, String[] models, int[] predictions) {
		String params = "reset&user_ids=";
		if(user_ids.length > 0) {
			params += user_ids[0];
		}
		for(int i = 1; i < user_ids.length; i++) {
			params += "_" + user_ids[i];
		}
		params += "&models=";
		if(models.length > 0) {
			params += models[0];
		}
		for(int i = 1; i < models.length; i++) {
			params += "_" + models[i];
		}
		params += "&predictions=";
		if(predictions.length > 0) {
			params += predictions[0];
		}
		for(int i = 1; i < predictions.length; i++) {
			params += "_" + predictions[i];
		}
		return params;
	}
	
	public static void printGroundTruth(String input, String output, boolean header, int cmax) {
		int oldphase = 0;
		int direction = 1;
		int userid = 2;
		int taskname = 3;
		int x = 4;
		int y = 5;
		int zoom = 6;
		int phase = 7;
		int added = 8;
		int deleted = 9;
		
		String splitBy = "\t";
		BufferedReader br;
		BufferedWriter bw;
		try {
			br = new BufferedReader(new FileReader(input));
			bw = new BufferedWriter(new FileWriter(output));
			String user = null;
			if(header) br.readLine(); //get rid of header
			String line = br.readLine();
			
			int prevzoom = -1;
			List<DirectionClass> hist = new ArrayList<DirectionClass>();
			
			int incount = 0;
			int outcount = 0;
			int pancount = 0;
			String prev = null;
			

			bw.write("user\ttaskname\tx\ty\tzoom\tprevdirection\tdirection\tincount\toutcount\tpancount\tphase");
			bw.newLine();
			
			while(line!=null){
				String[] b = line.split(splitBy);
				if(b[deleted].equals("1")) {
					line = br.readLine();
					continue;
				}
				
				String ustring = b[userid];
				String currtask = b[taskname];
				int currzoom = Integer.parseInt(b[zoom]);
				int currx = Integer.parseInt(b[x]);
				int curry = Integer.parseInt(b[y]);
				String currphase = b[phase];
				String currdir = b[direction];
				
				if(user == null || !ustring.equals(user)) {
					user = ustring;
					hist.clear();
					incount = 0;
					outcount = 0;
					pancount = 0;
					prevzoom = -1;
				}
				
				if(prevzoom > -1) {
					//System.out.println("here");
					int zoomdiff = currzoom - prevzoom;
					//System.out.println("zoomdiff: "+zoomdiff);
					if(zoomdiff == 0) { // pan
						hist.add(DirectionClass.PAN);
						pancount++;
					} else if (zoomdiff > 0) { // currzoom > prevzoom -> zoom in
						hist.add(DirectionClass.IN);
						incount++;
					} else {
						hist.add(DirectionClass.OUT);
						outcount++;
					}
					
					if(hist.size() > cmax) {
						//System.out.println("here");
						DirectionClass toRemove = hist.remove(0);
						if(toRemove == DirectionClass.IN) {
							incount--;
						} else if (toRemove == DirectionClass.OUT) {
							outcount--;
						} else {
							pancount--;
						}
					}
				}
				
				prevzoom = currzoom;
				
				System.out.print(ustring+"\t"+currtask+"\t"+currx+"\t"+curry+"\t"+currzoom+"\t"+prev+"\t"+currdir);
				// print the counts of zooms and pans for the last X steps
				int max = Math.max(incount+outcount+pancount, 1);
				System.out.print("\t"+incount);
				System.out.print("\t"+outcount);
				System.out.print("\t"+pancount);
				System.out.print("\t"+currphase);
				System.out.println();
				
				bw.write(ustring+"\t"+currtask+"\t"+currx+"\t"+curry+"\t"+currzoom+"\t"+prev+"\t"+currdir);
				bw.write("\t"+incount);
				bw.write("\t"+outcount);
				bw.write("\t"+pancount);
				bw.write("\t"+currphase);
				bw.newLine();
				prev = currdir;
				line = br.readLine();
			}
			br.close();
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void testsequence() throws Exception {
		sendRequest("[0, 0]", 0, "123");
		sendRequest("[0, 0]", 0, "123");
		//sendRequest("[0, 0]", 1, "123");
		//sendRequest("[0, 1]", 2, "123");
		//sendRequest("[0, 2]", 3, "123");
	}

	public static void main(String[] args) throws Exception {
		int[] user_ids = null;
        String[] tasknames = null;
        String[][] models = null;
        //int predictions = 1;
        int[][] allocations = new int[1][];
        boolean test = true;
        boolean all = false;
        boolean print = false;
        boolean groundtruth = false;
        String gtf = "";
		
		if(args.length < 1) return; // nothing to do!
		
		if(args[0].equals("print")) {
			print = true;
		} else if (args[0].equals("groundtruth")) {
			groundtruth = true;
			gtf = args[1];
	    } else {
			backend_port = Integer.parseInt(args[0]);
			List<String> newArgs = new ArrayList<String>();
			for(int i = 1; i < args.length; i++) {
				newArgs.add(args[i]);
			}
			if(newArgs.size() > 0) {
				if((newArgs.size() == 3) || (newArgs.size() == 4)) {
					String[] useridstrs = newArgs.get(0).split(",");
					user_ids = new int[useridstrs.length];
					for(int i = 0; i < useridstrs.length; i++) {
						user_ids[i] = Integer.parseInt(useridstrs[i]);
						//System.out.println("adding user: "+user_ids[i]);
					}
	
					String[] taskstrs = newArgs.get(1).split(",");
					tasknames = new String[taskstrs.length];
					for(int i = 0; i < taskstrs.length; i++) {
						tasknames[i] = taskstrs[i];
						//System.out.println("adding task: "+tasknames[i]);
					}
					
					String[] modelstrs = newArgs.get(2).split("-");
                    models = new String[modelstrs.length][];
                    for(int i = 0; i < modelstrs.length; i++) {
                            String[] temp = modelstrs[i].split(",");
                            models[i] = new String[temp.length];
                            System.out.print("adding model combination:");
                            for(int j=0; j < temp.length;j++) {
                                    models[i][j] = temp[j];
                                    System.out.print(" "+models[i][j]);
                            }
                            System.out.println();
                    }
					
					if(newArgs.size() == 4) {
						// use predictions as space allocations
						String[] tempallocations = newArgs.get(3).split("-"); // for each model combo
						if(tempallocations.length != models.length) {
							System.out.println("Not enough allocations!");
							return;
						}
						allocations = new int[tempallocations.length][];
						for(int i = 0; i < tempallocations.length; i++) {
							 String[] temp = tempallocations[i].split(",");
	                            allocations[i] = new int[temp.length];
	                            System.out.print("adding allocation combination:");
	                            for(int j=0; j < temp.length;j++) {
	                                    allocations[i][j] = Integer.parseInt(temp[j]);
	                                    System.out.print(" "+allocations[i][j]);
	                            }
	                            System.out.println();
						}
						//predictions = Integer.parseInt(newArgs.get(3));
					}
					
					test = false;
				} else if(newArgs.size() == 1) {
					if(newArgs.get(0).equals("all")) {
						all = true;
						test = false;
					} else if (newArgs.get(0).equals("print")) {
						test = false;
						print = true;
					}
				}
			}
		}
		if(groundtruth) {
			for(int len = 1; len <= 10; len++) {
				printGroundTruth(gtf,"out_"+len+".tsv",true,len);
			}
		} else if(print) {
			System.out.println("printing trace output");
			printTracesForSpecificUsers();
		}
		/*
        else if (test) {
                System.out.println("running simple sequence test");
                int[] users = {28};
                models = new String[1];
                models[0] ="normal";
                predictions = 1;
                System.out.println("reset?: "+sendReset(users,models,predictions));
                testsequence();
                //int[] testusers = {27};
                //String[] tasks = {"task1"};
                //getTracesForSpecificUsers(testusers,tasks);
        }
        */
		else if((user_ids != null) && (tasknames != null) && (models!=null)) {
            System.out.println("running specific trace tests");
            //getTracesForSpecificUsers(user_ids,tasknames,models,predictions);
            /*
            String[][] tm = new String[models.length][];
            for(int i = 0; i < models.length; i++) {
                    tm[i] = new String[1];
                    tm[i][0] = models[i];
            }
            */
            
            //int[] tp = {predictions};
            crossValidationModelSpecific(user_ids,tasknames,models,allocations);
		} else if(all) {
			System.out.println("testing all traces for all tasks");
			//getTracesForAllUsers();
			crossValidation1Model();
		}
	}
}
