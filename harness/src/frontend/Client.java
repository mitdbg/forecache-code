package frontend;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import backend.prediction.directional.MarkovDirectionalModel;
import backend.util.Direction;
import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;

public class Client {
	public static String backend_host = "localhost";
	public static int backend_port = 8080;
	public static String backend_root = "gettile";
	//public static String [] tasknames = {"warmup", "task1", "task2", "task3"};
	public static String [] tasknames = {"task1", "task2", "task3"};
	
	public static void crossValidation1Model() throws Exception {
		String[][] models = {{"normal"},{"histogram"},{"fhistogram"}};
		int[] predictions = {1,3,5};
		for(String taskname : tasknames) {
			for(int i = 0; i < models.length; i++) {
				for(int predict = 0; predict < predictions.length; predict++) {
					System.out.println("task: "+taskname+", model: "+models[i][0]+", predictions: "+predictions[predict]);
					crossValidation(taskname,models[i],predictions[predict]);
				}
			}
		}
	}
	
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
			//System.out.println();
			// setup test case on backend
			sendReset(trainlist,models,predictions);
			
			//send requests
			int user_id = finalusers.get(u1);
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id,taskname);
			for(int r = 0; r < trace.size(); r++) {
				UserRequest ur = trace.get(r);
				String tile_id = ur.tile_id;
				String tile_hash = ur.tile_hash;
				int zoom = ur.zoom;
				//System.out.println("tile id: '" +tile_id+ "'");
				sendRequest(tile_id,zoom,tile_hash);
			}
			
			//get accuracy for this user
			double accuracy = getAccuracy();
			overall_accuracy += accuracy;
			System.out.println(user_id+"\t"+accuracy);
		}
		overall_accuracy /= finalusers.size();
		System.out.println("overall\t"+overall_accuracy);
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
		List<Integer> user_ids = DBInterface.getUsersFromTraces();
		List<String> tasks = DBInterface.getTasksFromTraces();
		for(int u = 0; u < user_ids.size(); u++) {
			int user_id = user_ids.get(u);
			for(int t = 0; t < tasks.size(); t++) {
				String taskname = tasks.get(t);
				List<UserRequest> trace = DBInterface.getUserTraces(user_id,taskname);
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
							System.out.println(user_id+","+taskname+","+path_id+","+steps+","+dir);
						}
						prev = next;
					}
				}
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
				sendReset(user_ids,models,predictions);
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
				if(trace.size() > 0) {
					System.out.println("average time to recieve result: " + (average/trace.size())+"ms");
				}
				//}
			}
		}
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
	public static boolean sendReset(int[] user_ids, String[] models, int predictions) {
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
	
	public static String buildResetParams(int[] user_ids, String[] models, int predictions) {
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
		params+= "&predictions="+predictions;
		return params;
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
		String[] models = null;
		int predictions = 1;
		boolean test = true;
		boolean all = false;
		boolean print = false;
		if(args.length > 0) {
			if((args.length == 3) || (args.length == 4)) {
				String[] useridstrs = args[0].split(",");
				user_ids = new int[useridstrs.length];
				for(int i = 0; i < useridstrs.length; i++) {
					user_ids[i] = Integer.parseInt(useridstrs[i]);
					System.out.println("adding user: "+user_ids[i]);
				}

				String[] taskstrs = args[1].split(",");
				tasknames = new String[taskstrs.length];
				for(int i = 0; i < taskstrs.length; i++) {
					tasknames[i] = taskstrs[i];
					System.out.println("adding task: "+tasknames[i]);
				}
				
				String[] modelstrs = args[2].split(",");
				models = new String[modelstrs.length];
				for(int i = 0; i < modelstrs.length; i++) {
					models[i] = modelstrs[i];
					System.out.println("adding task: "+models[i]);
				}
				
				if(args.length == 4) {
					predictions = Integer.parseInt(args[3]);
				}
				
				test = false;
			} else if(args.length == 1) {
				if(args[0].equals("all")) {
					all = true;
					test = false;
				} else if (args[0].equals("print")) {
					test = false;
					print = true;
				}
			}
		}

		if(print) {
			System.out.println("printing trace output");
			printTracesForSpecificUsers();
		}else if (test) {
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
		} else if((user_ids != null) && (tasknames != null) && (models!=null)) {
			System.out.println("running specific trace tests");
			getTracesForSpecificUsers(user_ids,tasknames,models,predictions);
		} else if(all) {
			System.out.println("testing all traces for all tasks");
			//getTracesForAllUsers();
			crossValidation1Model();
		}
	}
}
