package implementation.modis;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import configurations.BigDawgConfig;
import configurations.Config;
import configurations.ModisConfig;
import configurations.VMConfig;

import utils.DBInterface;
import utils.ExplorationPhase;
import utils.TraceMetadata;
import utils.UserRequest;
import utils.UtilityFunctions;
import abstraction.storage.MainMemoryTileBuffer;
import backend.disk.NiceTilePacker;
import backend.memory.MemoryNiceTileLruBuffer;
import backend.util.DirectionClass;
import backend.util.ModelAccuracy;
import backend.util.NiceTile;
import backend.util.TileKey;

public class CachingClient extends Client {
	public static MainMemoryTileBuffer memcache;
	
	public static void crossValidationModelSpecific(int[] users, String[] tasknames, String[][] models, int[][] allocations, boolean[] usePhases) throws Exception {
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
				crossValidation(taskname,models[allc],testusers,allocations[allc], ma,usePhases[allc]);
			}
		}
	}

	// only test specific users, but train on all users
	public static void crossValidation(String taskname, String[] models, List<Integer> testusers, int[] predictions,
			ModelAccuracy[] ma, boolean usePhases) throws Exception {
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
			sendReset(trainlist,models,predictions,usePhases);
			memcache.clear(); // empty out the cache
			
			//send requests
			int user_id = testusers.get(u1);
			List<UserRequest> trace = DBInterface.getHashedTraces(user_id,taskname);
			long start = System.currentTimeMillis();
			double[] durations = new double[trace.size()];
			String[] updatedAccuracy = new String[durations.length];
			double avg_duration = 0;
			for(int r = 0; r < trace.size(); r++) {
				UserRequest ur = trace.get(r);
				String tile_id = ur.tile_id;
				//TODO: remove hard-coded delimiter reference
				int[] id = UtilityFunctions.parseTileIdInteger(tile_id);
				String tile_hash = ur.tile_hash;
				int zoom = ur.zoom;
				TileKey tempKey = new TileKey(id, zoom);
				//System.out.println("checking for tile "+tempKey);
				long s = System.currentTimeMillis();
				NiceTile toRetrieve = memcache.getTile(tempKey);
				long e = System.currentTimeMillis();
				if(toRetrieve == null) { // not cached on client
					//System.out.println("tile id: '" +tile_id+ "'");
					waitForServer();
					durations[r] = sendRequest(tile_id,zoom,tile_hash);
				} else { // found it on the client
					durations[r] = e-s;
					updatedAccuracy[r] = "client-hit";
					
					waitForServer();
					// tell the server which tile the user requested
					sendHistory(tile_id,zoom,tile_hash);
				}
				avg_duration += durations[r];
			}
			long end = System.currentTimeMillis();
			System.out.println("duration: "+(1.0*(end-start)/1000)+" secs");
			//get accuracy for this user
			double accuracy = getAccuracy();
			avg_duration /= trace.size();
			String[] fullAccuracy = getFullAccuracy();
			int curr = 0;
			for(int i = 0; i < durations.length; i++) {
				if(updatedAccuracy[i] == null) {
					updatedAccuracy[i] = "server-"+fullAccuracy[curr];
					curr++;
				}
			}
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
			System.out.print(accuracy);
			System.out.print("\t");
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
				UtilityFunctions.printStringArray(models);
				System.out.print("\t");
				UtilityFunctions.printIntArray(predictions);
				System.out.print("\t");
				System.out.print(request.zoom+"\t"+id[0]+"\t"+id[1]+"\t"+dirs.get(i)+"\t"+phases.get(i)+
						"\t"+updatedAccuracy[i]);
				//System.out.println("\t"+predictions+"\t"+request.zoom+"\t"+id[0]+"\t"+id[1]+"\t"+dirs.get(i)+"\t"+phases.get(i)+
				//		"\t"+fullAccuracy[i]);
				System.out.print("\t");
				System.out.println(durations[i]);
			}
			
		}
		overall_accuracy /= testusers.size();
		//System.out.println("overall\t"+overall_accuracy);
	}
	
	public static long sendHistory(String tile_id, int zoom, String hashed_query) throws Exception {
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
	
	public static long sendRequest(String tile_id, int zoom, String hashed_query) throws Exception {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?fetch&" + buildUrlParams(hashed_query, tile_id, zoom);
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
			//System.out.println("tile ("+tile_id+", "+zoom+") result length: " + result.length());
			if(result.equals("error")) {
				throw new Exception("serious error occurred on backend while retrieving tile");
			}
			byte[] rawBytes = buffer.toByteArray();
			NiceTile tile = NiceTilePacker.unpackNiceTile(rawBytes); // unpack tile
			//System.out.println("Inserting tile "+tile.id);
			memcache.insertTile(tile); // insert the new tile
			
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
	
	
	public static void main(String[] args) throws Exception {
		//set configurations
		Config conf;
		conf = new VMConfig();
		// conf = new BigDawgConfig();
		// conf = new ModisConfig();
		conf.setConfig();
		
		// setup the cache
		int cacheSize = 0;

		//same as original Client code
		int[] user_ids = null;
        String[] tasknames = null;
        String[][] models = null;
        //int predictions = 1;
        int[][] allocations = new int[1][];
        boolean test = true;
        boolean all = false;
        boolean print = false;
        boolean groundtruth = false;
        boolean[] usePhases = {false};
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
				if((newArgs.size() >= 3) && (newArgs.size() <= 6)) {
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
					
					if(newArgs.size() >= 4) {
						// use predictions as space allocations
						String[] tempallocations = newArgs.get(3).split("-"); // for each model combo
						if(tempallocations.length != models.length) {
							System.out.println("Not enough allocations!");
							return;
						}
						allocations = new int[tempallocations.length][];
						usePhases = new boolean[tempallocations.length];
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
					
					if(newArgs.size() >= 5) {
						String[] tempflags = newArgs.get(4).split("-"); // for each model combo
						if(tempflags.length != models.length) {
							System.out.println("Not enough usePhase flags!");
							return;
						}
						usePhases = new boolean[tempflags.length];
						for(int i = 0; i < tempflags.length; i++) {
							usePhases[i] = Boolean.parseBoolean(tempflags[i]);
							System.out.println("adding phase usage: "+usePhases[i]);
						}
					}
					
					if (newArgs.size() == 6) { // how big should the client-side cache be?
						cacheSize = Integer.parseInt(newArgs.get(5));
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
		
		// just evict tiles using lru
		if(cacheSize == 0) {
			System.out.println("warning: cache size is zero. ignoring client-side cache...");
		} else {
			System.out.println("using client-side cache of size " + cacheSize+"...");
		}
		memcache = new MemoryNiceTileLruBuffer(cacheSize);
		
		if(groundtruth) {
			for(int len = 1; len <= 10; len++) {
				Client.printGroundTruth(gtf,"out_"+len+".tsv",true,len);
			}
		} else if(print) {
			System.out.println("printing trace output");
			Client.printTracesForSpecificUsers();
		}
		else if((user_ids != null) && (tasknames != null) && (models!=null)) {
            System.out.println("running specific trace tests");
            crossValidationModelSpecific(user_ids,tasknames,models,allocations,usePhases);
		} else if(all) {
			System.out.println("testing all traces for all tasks");
			crossValidation1Model();
		}
	}
}
