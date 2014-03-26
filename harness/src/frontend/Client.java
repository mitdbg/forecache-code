package frontend;

import java.net.*;
import java.io.*;
import java.util.List;

import utils.DBInterface;
import utils.UtilityFunctions;
import frontend.UserRequest;

public class Client {
	public static String backend_host = "localhost";
	public static int backend_port = 8080;
	public static String backend_root = "gettile";
	public static String [] tasknames = {"warmup", "task1", "task2", "task3"};
	
	public static void getTracesForUsers() {
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
						System.out.println("tile id: '" +tile_id+ "'");
						
						// do stuff here to analyze trace
						
						
					}
				}
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
			System.out.println("result: " + result);
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
	
	public static void test() {
		//System.out.println("params:" +buildUrlParams("123","[1, 25]", 0));
		for(int z = 0; z < 5; z++) {
			for(int i = 0; i <= z; i++) {
				for(int j = 0; j <= z; j++) {
					//sendRequest("[0, 0]", 0, "123");
					sendRequest("["+i+", "+j+"]", z, "123");
				}
			}
		}
	}
	
	public static void main(String[] args) {
		/*
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Could not find JDBC driver");
			e.printStackTrace();
			return;
		}
		
		Connection conn = DBInterface.getConnection();
		if(conn == null) {
			return;
		}
		*/
		//getTracesForUsers(conn);
		test();
	}
}
