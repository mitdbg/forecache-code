package implementation.modis;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opencv.core.Core;

import configurations.BigDawgConfig;
import configurations.Config;
import configurations.ModisConfig;
import configurations.VMConfig;

import backend.BuildSignaturesOffline;
import backend.PreCompThread;
import backend.PredictionManager;
import backend.disk.DiskNiceTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryNiceTileLruBuffer;
import backend.prediction.TileHistoryQueue;
import backend.util.DirectionClass;
import backend.util.ModelAccuracy;
import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.SignatureMap;
import backend.util.TileKey;
import utils.DBInterface;
import utils.ExplorationPhase;
import utils.TraceMetadata;
import utils.UserRequest;
import utils.UtilityFunctions;

public class PreCompClient {
	public static String backend_host = "localhost";
	public static int backend_port = 8080;
	public static String backend_root = "gettile";
	//public static String [] tasknames = {"warmup", "task1", "task2", "task3"};
	public static CacheLevelParameterManager clientParamsManager = null;
	public static CacheLevelParameterManager memParamsManager = null;
	public static CacheLevelParameterManager pcParamsManager = null;
	public static boolean useclient = false;
	public static boolean usemem = false;
	public static boolean usepc = false;
	public static int clientIndex;
	public static int memIndex;
	public static int diskIndex;
	
	//for prediction
	public static int deflmbuflen = 0; //default is don't use
	public static int histmax = 10;
	public static DiskNiceTileBuffer diskbuf;
	public static PredictionManager clientManager;
	public static ScidbTileInterface scidbapi;
	public static TileHistoryQueue hist;
	public static SignatureMap sigMap;
	private static ExecutorService executorService;
	
	public static void initExecutorService() {
		executorService = (ExecutorService) Executors.newFixedThreadPool(2);
	}
	
	public static void shutdownExecutorService() {
		executorService.shutdown();
	}
	
	public static void cancelPredictions() throws Exception {
		if(useclient) clientManager.cancelPredictorJob();
	}
		
	public static void crossValidationMultiLevel() throws Exception {
		int[] users = new int[0];
		String[] tasknames = new String[0];
		
		clientIndex = 0;
		memIndex = 0;
		diskIndex = 0;
		int clientmax = 1;
		int memmax = 1;
		int diskmax = 1;
		if(useclient) {
			clientmax = clientParamsManager.usePhases.length;
			tasknames = clientParamsManager.tasknames;
			users = clientParamsManager.user_ids;
		}
		if(usemem) {
			memmax = memParamsManager.usePhases.length;
			tasknames = memParamsManager.tasknames;
			users = memParamsManager.user_ids;
		}
		if(usepc) {
			diskmax = pcParamsManager.usePhases.length;
			tasknames = pcParamsManager.tasknames;
			users = pcParamsManager.user_ids;
		}
		if(users.length == 0) throw new Exception("List of users is empty!");
		if(tasknames.length == 0) throw new Exception("List of tasks is empty!");
		
		List<Integer> testusers = new ArrayList<Integer>();
		for(int i = 0; i < users.length; i++) {
			testusers.add(users[i]);
		}
		
		for(String taskname : tasknames) {
			for(clientIndex = 0; clientIndex < clientmax; clientIndex++) {
				for(memIndex = 0; memIndex < memmax; memIndex++) {
					for(diskIndex = 0; diskIndex < diskmax; diskIndex++) {
						doMultiLevelValidation(testusers,taskname);
					}
				}
			}
		}
	}
	
	public static void doMultiLevelValidation(List<Integer> testusers,
			String taskname) throws Exception {
		List<Integer> users = DBInterface.getUsers();
		System.out.println("total users: "+users.size());
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
			
			//setup test case on frontend
			if(useclient) {
				// TODO: fill this in for multi-level follow-on work
				clientManager.reset(trainlist, clientParamsManager.models[clientIndex],
						clientParamsManager.allocations[clientIndex],
						clientParamsManager.neighborhoods[clientIndex],
						clientParamsManager.usePhases[clientIndex]);
			}

			// setup test case on backend
			sendCacheLevelFlags(usemem, usepc); // are we using these?

			if(usemem) sendReset(memParamsManager.level,trainlist,memParamsManager.models[memIndex],
				memParamsManager.allocations[memIndex],memParamsManager.neighborhoods[memIndex],
				memParamsManager.usePhases[memIndex]);
			
			if(usepc) sendReset(pcParamsManager.level,trainlist,pcParamsManager.models[diskIndex],
				pcParamsManager.allocations[diskIndex],pcParamsManager.neighborhoods[diskIndex],
				pcParamsManager.usePhases[diskIndex]);

			//send requests
			int user_id = testusers.get(u1);
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id,taskname);
			long start = System.currentTimeMillis();
			double[] durations = new double[trace.size()];
			double avg_duration = 0;
			for(int r = 0; r < trace.size(); r++) {
				UserRequest ur = trace.get(r);
				String tile_id = ur.tile_id;
				String tile_hash = ur.tile_hash;
				int zoom = ur.zoom;
				//System.out.println("tile id: '" +tile_id+ "'");
				
				NiceTile toRetrieve = null;
				TileKey tempKey = new TileKey(UtilityFunctions.parseTileIdInteger(tile_id),zoom);
				long s = 0;
				long e = 0;

				if(useclient) {
					//TODO: add code to toggle for cancelling job(s) instead of waiting
					waitForClientPredictor();
					clientManager.updateAccuracy(tempKey);
					s = System.currentTimeMillis();
					toRetrieve = clientManager.buf.getTile(tempKey);
					e = System.currentTimeMillis();
					addHistory(tempKey);
				}
				
				//TODO: add code to toggle for cancelling job(s) instead of waiting
				waitForServer();
				if(toRetrieve == null) { // not cached on client
					//System.out.println("tile id: '" +tile_id+ "'");
					durations[r] = sendRequest(tile_id,zoom,tile_hash);
				} else { // found it on the client
					durations[r] = e-s;
					// only add to LRU buffer if it was accessed in this cache
					clientManager.lmbuf.insertTile(toRetrieve);
					// tell the server which tile the user requested
					sendHistory(tile_id,zoom,tile_hash);
				}
				if(useclient) clientManager.runPredictor(executorService);
				
				avg_duration += durations[r];
			}
			long end = System.currentTimeMillis();
			System.out.println("duration: "+(1.0*(end-start)/1000)+" secs");
			//get accuracy for this user
			if(useclient) {
				clientParamsManager.accuracy = clientManager.getAccuracy();
				clientParamsManager.fullAccuracy = clientManager.getFullAccuracyRaw();
			}
			
			if(usemem) {
				memParamsManager.accuracy = getAccuracy(memParamsManager.level);
				memParamsManager.fullAccuracy = getFullAccuracy(memParamsManager.level);
			}
			
			if(usepc) {
				pcParamsManager.accuracy = getAccuracy(pcParamsManager.level);
				pcParamsManager.fullAccuracy = getFullAccuracy(pcParamsManager.level);
			}
			
			avg_duration /= trace.size();
			
			/*
			//original printed content
			System.out.print(user_id+"\t");
			System.out.print(taskname+"\t");
			if(useclient) {
				UtilityFunctions.printStringArray(clientManager.models[clientIndex]);
				System.out.print("\t");
				UtilityFunctions.printIntArray(clientManager.allocations[clientIndex]);
				System.out.print("\t");
			}
			
			if(usemem) {
				UtilityFunctions.printStringArray(memManager.models[memIndex]);
				System.out.print("\t");
				UtilityFunctions.printIntArray(memManager.allocations[memIndex]);
				System.out.print("\t");
			}
			
			if(usepc) {
				UtilityFunctions.printStringArray(diskManager.models[diskIndex]);
				System.out.print("\t");
				UtilityFunctions.printIntArray(diskManager.allocations[diskIndex]);
				System.out.print("\t");
			}
			UtilityFunctions.printStringArray(clientManager.fullAccuracy);
			System.out.print("\t");
			UtilityFunctions.printStringArray(memManager.fullAccuracy);
			System.out.print("\t");
			UtilityFunctions.printStringArray(diskManager.fullAccuracy);
			System.out.print("\t");
			System.out.print(clientManager.accuracy+"\t");
			System.out.print(memManager.accuracy+"\t");
			System.out.print(diskManager.accuracy+"\t");
			System.out.println(avg_duration);
			*/
			
			// new printed content
			TraceMetadata metadata = RequestLabeler.getLabels(trace);
			List<DirectionClass> dirs = metadata.directionClasses;
			List<ExplorationPhase> phases = metadata.explorationPhases;
			for(int i = 0; i < trace.size(); i++) {
				UserRequest request = trace.get(i);
				int[] id = UtilityFunctions.parseTileIdInteger(request.tile_id);
				System.out.print(user_id+"\t"+taskname+"\t");
				System.out.print(useclient+"\t"+usemem+"\t"+usepc+"\t");
				
				if(useclient) UtilityFunctions.printStringArray(
						clientParamsManager.models[clientIndex]);
				System.out.print("\t");
				if(useclient) UtilityFunctions.printIntArray(
						clientParamsManager.allocations[clientIndex]);
				System.out.print("\t");
				
				if(usemem) UtilityFunctions.printStringArray(memParamsManager.models[memIndex]);
				System.out.print("\t");
				if(usemem) UtilityFunctions.printIntArray(memParamsManager.allocations[memIndex]);
				System.out.print("\t");
				
				if(usepc) UtilityFunctions.printStringArray(pcParamsManager.models[diskIndex]);
				System.out.print("\t");
				if(usepc) UtilityFunctions.printIntArray(pcParamsManager.allocations[diskIndex]);
				System.out.print("\t");
				
				System.out.print(request.zoom+"\t"+id[0]+"\t"+id[1]+
						"\t"+dirs.get(i)+"\t"+phases.get(i)+"\t");
				
				if(useclient) System.out.print(clientParamsManager.fullAccuracy[i]);
				System.out.print("\t");
				if(usemem) System.out.print(memParamsManager.fullAccuracy[i]);
				System.out.print("\t");
				if(usepc) System.out.print(pcParamsManager.fullAccuracy[i]);
				System.out.print("\t");
				//System.out.println("\t"+predictions+"\t"+request.zoom+"\t"+id[0]+
				//		"\t"+id[1]+"\t"+dirs.get(i)+"\t"+phases.get(i)+
				//		"\t"+fullAccuracy[i]);
				System.out.print("\t");
				System.out.println(durations[i]);
			}
			
		}
	}
	
	public static void waitForClientPredictor() throws InterruptedException {
		for(int i = 0; i < 10000; i++) {
			if(!clientManager.isReady()) {
				//System.out.println("not ready... waiting 100ms");
				Thread.sleep(100);
			} else {
				//System.out.println("continuing on...");
				break;
			}
		}
	}
	
	public static void waitForServer() throws Exception {
		for(int i = 0; i < 10000; i++) {
			if(!checkReady()) {
				//System.out.println("not ready... waiting 100ms");
				Thread.sleep(100);
			} else {
				//System.out.println("continuing on...");
				break;
			}
		}
	}
	
	// tell server what user ids and models to train on
	public static String[] getFullAccuracy(CacheLevel level) {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?fullaccuracy&level="+level;
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
	public static double getAccuracy(CacheLevel level) {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?accuracy&level="+level;
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
		public static boolean sendCacheLevelFlags(boolean usemem, boolean usepc) {
			String urlstring = "http://"+backend_host+":"+backend_port
					+"/"+backend_root + "/?cachelevels";
			if(usemem) urlstring += "&usemem";
			if (usepc) urlstring += "&usepc";
			
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

	// tell server what user ids and models to train on
	public static boolean sendReset(CacheLevel level, int[] user_ids, String[] models,
			int[] predictions, int neighborhood,boolean usePhases) {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?"+buildResetParams(level,user_ids,models, predictions, neighborhood,usePhases);
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
	
	public static boolean checkReady() throws Exception {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?ready";
		URL geturl = null;
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuffer sbuffer = new StringBuffer();
		String line;
		String result = null;
		boolean ready = false;
		try {
			geturl = new URL(urlstring);
		} catch (MalformedURLException e) {
			System.out.println("error occurred while retrieving url object for: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(geturl == null) {
			return ready;
		}

		try {
			connection = (HttpURLConnection) geturl.openConnection();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while((line = reader.readLine()) != null) {
				sbuffer.append(line);
			}
			reader.close();
			result = sbuffer.toString();
			ready = Boolean.parseBoolean(result);
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
		return ready;
	}
	
	public static long sendHistory(String tile_id, int zoom,
			String hashed_query) throws Exception {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?addhistory&" + buildUrlParams(hashed_query, tile_id, zoom);
		URL geturl = null;
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuffer sbuffer = new StringBuffer();
		String result = null;
		long diff = 0;
		try {
			geturl = new URL(urlstring);
		} catch (MalformedURLException e) {
			System.out.println("error occurred while retrieving url object for: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(geturl == null) {
			return diff;
		}

		try {
			connection = (HttpURLConnection) geturl.openConnection();
			diff = System.currentTimeMillis();
			InputStream is = connection.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
			  buffer.write(data, 0, nRead);
			}

			buffer.flush();
			diff = System.currentTimeMillis() - diff;
			result = sbuffer.toString();
			if(result.equals("error")) {
				throw new Exception("serious error occurred on backend while sending history");
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
		return diff;
	}

	public static long sendRequest(String tile_id, int zoom,
			String hashed_query) throws Exception {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?fetch&" + buildUrlParams(hashed_query, tile_id, zoom);
		URL geturl = null;
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuffer sbuffer = new StringBuffer();
		String line;
		String result = null;
		long diff = 0;
		try {
			geturl = new URL(urlstring);
		} catch (MalformedURLException e) {
			System.out.println("error occurred while retrieving url object for: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(geturl == null) {
			return diff;
		}

		try {
			connection = (HttpURLConnection) geturl.openConnection();
			diff = System.currentTimeMillis();
			InputStream is = connection.getInputStream();
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
			  buffer.write(data, 0, nRead);
			}

			buffer.flush();
			diff = System.currentTimeMillis() - diff;
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
		return diff;
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
	
	public static String buildResetParams(CacheLevel level, int[] user_ids, String[] models,
			int[] predictions, int neighborhood, boolean usePhases) {
		String params = "reset&user_ids=";
		if(level != null) {
			params = "level="+level+"&"+params;
		}
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
		params +="&neighborhood="+neighborhood;
		if(usePhases) {
			params += "&usephases=true";
		}
		return params;
	}
	
	public static void initializeCacheManagers(PredictionManager manager,
			NiceTileBuffer buf, NiceTileBuffer lmbuf) throws Exception {		
		// initialize cache managers
		manager.buf = buf;
		manager.lmbuf = lmbuf; // tracks the user's last x moves
		
		manager.diskbuf = diskbuf; // just used for convenience
		manager.dbapi = scidbapi;
		manager.hist = hist;
		
		// load pre-computed signature map
		manager.sigMap  = sigMap;
		
		//setup models for prediction
		manager.setupModels();
		manager.trainModels();
	}
	
	public static void testsequence() throws Exception {
		sendRequest("[0, 0]", 0, "123");
		sendRequest("[0, 0]", 0, "123");
		//sendRequest("[0, 0]", 1, "123");
		//sendRequest("[0, 1]", 2, "123");
		//sendRequest("[0, 2]", 3, "123");
	}
	
	public static void addHistory(TileKey key) {
		// get the tile, so we can put it in the history
					NiceTile t = diskbuf.getTile(key);
					if(t == null) { // check dbms
						t = new NiceTile();
						t.id = key;
						scidbapi.getStoredTile(DBInterface.arrayname, t);
					}
					
					hist.addRecord(t); // keep track
	}
	
	public static void main(String[] args) throws Exception {
		//set configurations
		Config conf;
		conf = new VMConfig();
		// conf = new BigDawgConfig();
		// conf = new ModisConfig();
		conf.setConfig();
		
		if(args.length < 1) throw new Exception("No parameters passed!"); // nothing to do!
		if(args.length != 8) throw new Exception("Only "+args.length+" parameters passed! Need 8.");
		
		backend_port = Integer.parseInt(args[0]);
		String userstring = args[1];
		String taskstring = args[2];
		String raw_levelstring = args[3];
		String raw_modelstring = args[4];
		String raw_allocationstring = args[5];
		String raw_neighborhoodstring = args[6];
		String raw_phasestring = args[7];
		
		String[] levels = raw_levelstring.split("/");
		String[] models = raw_modelstring.split("/");
		String[] allocations = raw_allocationstring.split("/");
		String[] neighborhoods = raw_neighborhoodstring.split("/");
		String[] phases = raw_phasestring.split("/");
		String message = null;
		if(allocations.length != phases.length) {
			message = "Mismatched number of multi-level parameters: check allocations vs. phases";
		} else if (phases.length != models.length) {
			message = "Mismatched number of multi-level parameters: check phases vs. models";
		} else if(models.length != levels.length) {
			message = "Mismatched number of multi-level parameters: check models vs. levels";
		} else if(levels.length != neighborhoods.length) {
			message = "Mismatched number of multi-level parameters: check levels vs. neighborhoods";
		}
		if(message != null) throw new ParameterException.ClientInputException(message);
		/*
		if((allocations.length != phases.length) ||
				(phases.length != models.length) ||
				(models.length != levels.length) ||
				(levels.length != neighborhoods.length)) {
			throw new Exception("Mismatched number of multi-level parameters passed!");
		}*/
		Map<CacheLevel,Boolean> alreadySpecified = new HashMap<CacheLevel,Boolean>();
		for(int i = 0; i < levels.length; i++) {
			CacheLevel l = UtilityFunctions.getCacheLevel(levels[i]);
			if(alreadySpecified.containsKey(l)) {
				throw new Exception("Duplicate set of parameters " +
						"specified for caching level '"+l+"'!");
			}
			switch(l) {
			case CLIENT:
				useclient = true;
				System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
				clientParamsManager = new CacheLevelParameterManager(userstring,taskstring,
						l,models[i],allocations[i],neighborhoods[i],phases[i]);
				
				clientManager = new PredictionManager();
				MemoryNiceTileBuffer clientbuf = new MemoryNiceTileBuffer();
				MemoryNiceTileLruBuffer lmbuf = new MemoryNiceTileLruBuffer(deflmbuflen);
				
				diskbuf = new DiskNiceTileBuffer(DBInterface.nice_tile_cache_dir,
						DBInterface.hashed_query,DBInterface.threshold);
				scidbapi = new ScidbTileInterface(DBInterface.defaultparamsfile,
						DBInterface.defaultdelim);
				hist = new TileHistoryQueue(histmax);
				// tracks the user's last x moves
				sigMap  = SignatureMap.getFromFile(BuildSignaturesOffline.defaultFilename);
				
				initializeCacheManagers(clientManager,clientbuf,lmbuf);
				initExecutorService();
				break;
			case SERVERMM:
				memParamsManager = new CacheLevelParameterManager(userstring,taskstring,
						l,models[i],allocations[i],neighborhoods[i],phases[i]);
				usemem = true;
				break;
			case SERVERDISK:
				pcParamsManager = new CacheLevelParameterManager(userstring,taskstring,
						l,models[i],allocations[i],neighborhoods[i],phases[i]);
				usepc = true;
				break;
			}
			alreadySpecified.put(l, true);
		}

		if(!useclient && !usemem && !usepc) throw new Exception("no caching flags set!");
		PreCompClient.crossValidationMultiLevel();
		if(useclient) shutdownExecutorService();
	}
}
