package frontend;

import java.net.*;
import java.io.*;
import java.util.List;

import backend.prediction.directional.MarkovDirectionalModel;

import utils.DBInterface;
import utils.UserRequest;
import utils.UtilityFunctions;

public class Client {
	public static String backend_host = "localhost";
	public static int backend_port = 8080;
	public static String backend_root = "gettile";
	public static String [] tasknames = {"warmup", "task1", "task2", "task3"};
	
	public static void getTracesForAllUsers() {
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
	
	public static void getTracesForSpecificUsers(int[] user_ids, String[] tasks) {
		for(int u = 0; u < user_ids.length; u++) {
			int user_id = user_ids[u];
			for(int t = 0; t < tasks.length; t++) {
				String taskname = tasks[t];
				System.out.println("checking task '"+taskname+"' for user "+user_id);
				//if(DBInterface.checkTask(user_id,taskname)) {
					System.out.println("user '" + user_id + "' completed task '" + taskname + "'");
					List<UserRequest> trace = DBInterface.getHashedTraces(user_id,taskname);
					System.out.println("found trace of size " + trace.size() + " for task '" + taskname + "' and user '" + user_id + "'");
					long average = 0;
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
					if(trace.size() > 0) {
						System.out.println("average time to recieve result: " + (average/trace.size())+"ms");
					}
				//}
			}
		}
	}
	
	public static void sendRequest(String tile_id, int zoom, String hashed_query) {
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
			System.out.println("tile ("+tile_id+", "+zoom+") result length: " + result.length());
		} catch (IOException e) {
			System.out.println("Error retrieving response from url: '"+urlstring+"'");
			e.printStackTrace();
		}
		
	}
	
	public static String buildUrlParams(String hashed_query, String tile_id, int zoom) {
		String params = "";
		//params += "hashed_query="+hashed_query;
		params += "&threshold=" + DBInterface.threshold;
		params += "&zoom=" + zoom;
		//List<Integer> tile_ints = DBInterface.parseTileIdInteger(tile_id);
		String stripped = tile_id.substring(1, tile_id.length() - 1);
		params += "&tile_id=" + UtilityFunctions.urlify(stripped);
		return params;
	}
	
	public static void testsequence() {
		sendRequest("[0, 0]", 0, "123");
		sendRequest("[0, 0]", 1, "123");
		sendRequest("[0, 1]", 2, "123");
		sendRequest("[0, 2]", 3, "123");
	}
	
	public static void main(String[] args) {
		int[] user_ids = null;
		String[] tasknames = null;
		boolean test = true;
		boolean all = false;
		if(args.length > 0) {
			if(args.length == 2) {
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
				test = false;
			} else if(args.length == 1) {
				if(args[0].equals("all")) {
					all = true;
					test = false;
				}
			}
		}

		if (test) {
			System.out.println("running simple sequence test");
			testsequence();
		} else if((user_ids != null) && (tasknames != null)) {
			System.out.println("running specific trace tests");
			getTracesForSpecificUsers(user_ids,tasknames);
		} else if(all) {
			System.out.println("testing all traces for all tasks");
			getTracesForAllUsers();
		}
	}
}
