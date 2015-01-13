package backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.opencv.core.Core;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.NiceTilePacker;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.TileHistoryQueue;
import backend.prediction.TrainModels;
import backend.prediction.directional.HotspotDirectionalModel;
import backend.prediction.directional.MomentumDirectionalModel;
import backend.prediction.directional.NGramDirectionalModel;
import backend.prediction.directional.RandomDirectionalModel;
import backend.prediction.signature.DenseSiftSignatureModel;
import backend.prediction.signature.FilteredHistogramSignatureModel;
import backend.prediction.signature.HistogramSignatureModel;
import backend.prediction.signature.NormalSignatureModel;
import backend.prediction.signature.SiftSignatureModel;
import backend.util.Model;
import backend.util.NiceTile;
import backend.util.TileKey;
import utils.DBInterface;
import utils.UtilityFunctions;

public class MainThread {
	public static MemoryNiceTileBuffer membuf;
	public static DiskNiceTileBuffer diskbuf;
	public static ScidbTileInterface scidbapi;
	public static int histmax = 10;
	public static TileHistoryQueue hist;
	
	//server
	public static Server server;
	
	//accuracy
	public static int total_requests = 0;
	public static int cache_hits = 0;
	public static List<String> hitslist = new ArrayList<String>();
	
	// General Model variables
	public static int defaultpredictions = 3;
	public static int defaulthistorylength = 4;
	public static int defaultport = 8080;
	public static int[] allocatedStorage; // storage per model
	public static int defaultstorage = 1; // default storage per model
	public static int neighborhood = 1; // default neighborhood from which to pick candidates
	
	public static Model[] modellabels = {Model.MOMENTUM};
	public static int[] historylengths = {defaulthistorylength};
	public static String taskname = "task1";
	public static int[] user_ids = {28};
	
	// global model objects	
	public static BasicModel[] all_models;
	
	public static void setupModels() {
		all_models = new BasicModel[modellabels.length];
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			switch(label) {
				case NGRAM: all_models[i] = new NGramDirectionalModel(hist,membuf,diskbuf,scidbapi,historylengths[i]);
				break;
				case RANDOM: all_models[i] = new RandomDirectionalModel(hist,membuf,diskbuf,scidbapi,historylengths[i]);
				break;
				case HOTSPOT: all_models[i] = new HotspotDirectionalModel(hist,membuf,diskbuf,scidbapi,historylengths[i],HotspotDirectionalModel.defaulthotspotlen);
				break;
				case MOMENTUM: all_models[i] = new MomentumDirectionalModel(hist,membuf,diskbuf,scidbapi,historylengths[i]);
				break;
				case NORMAL: all_models[i] = new NormalSignatureModel(hist,membuf,diskbuf,scidbapi,historylengths[i]);
				break;
				case HISTOGRAM: all_models[i] = new HistogramSignatureModel(hist,membuf,diskbuf,scidbapi,historylengths[i]);
				break;
				case FHISTOGRAM: all_models[i] = new FilteredHistogramSignatureModel(hist,membuf,diskbuf,scidbapi,historylengths[i]);
				break;
				case SIFT: all_models[i] = new SiftSignatureModel(hist,membuf,diskbuf,scidbapi,historylengths[i]);
				break;
				case DSIFT: all_models[i] = new DenseSiftSignatureModel(hist,membuf,diskbuf,scidbapi,historylengths[i]);
				default://do nothing, will fail if we get here
			}
		}
	}
	
	public static void trainModels() {
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			BasicModel mod = all_models[i];
			switch(label) {
				case NGRAM: TrainModels.TrainNGramDirectionalModel(user_ids, taskname, (NGramDirectionalModel) mod);
				break;
				case HOTSPOT: TrainModels.TrainHotspotDirectionalModel(user_ids, taskname, (HotspotDirectionalModel) mod);
				break;
				default://do nothing
			}
		}
	}
	
	public static void doPredictions() {
		if(all_models.length == 0) return;
		Map<TileKey,Boolean> toInsert = new HashMap<TileKey,Boolean>();
		
		// get the current list of candidates
		List<TileKey> candidates = all_models[0].getCandidates(neighborhood);
		
		for(int m = 0; m < modellabels.length; m++) { // for each model
			Model label = modellabels[m];
			BasicModel mod = all_models[m];
			List<TileKey> orderedCandidates = mod.orderCandidates(candidates);
			int count = 0;
			for(int i = 0; i < orderedCandidates.size(); i++) {
				if(count == allocatedStorage[m]) break;
				TileKey key = orderedCandidates.get(i);
				if(!toInsert.containsKey(key)) {
					toInsert.put(key, true);
					count++;
				}
			}

			//System.out.print("predicted ordering for model "+label+": ");
			//for(int i = 0; i < orderedCandidates.size(); i++) {
			//	System.out.print(orderedCandidates.get(i)+" ");
			//}
			//System.out.println();
		}
		System.out.print("predictions:");
		for(TileKey k : toInsert.keySet()) {
			System.out.print(k+" ");
		}
		System.out.println();
		Set<TileKey> oldKeys = new HashSet<TileKey>();
		for(TileKey k : membuf.getAllTileKeys()) {
			oldKeys.add(k);
		}
		//for(TileKey old : oldKeys) {
		//	if (!toInsert.containsKey(old)) {
		//		membuf.removeTile(old);
		//	}
		//}
		membuf.clear();
		List<TileKey> insertList = new ArrayList<TileKey>();
		insertList.addAll(toInsert.keySet());
		insertPredictions(insertList);
	}
	
	public static List<TileKey> getPredictions() {
		Map<TileKey,Double> predictions = new HashMap<TileKey,Double>();
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			BasicModel mod = all_models[i];
			Double basevote = 1.0; // value of a vote from this model
			List<TileKey> toadd = mod.predictTiles(defaultpredictions);
			
			if(toadd != null){
				System.out.print("predictions for model "+label+": ");
				for(TileKey k : toadd) {
					System.out.print(k+" ");
				}
				System.out.println();
			}
			
			// count votes per prediction scheme
			if((toadd != null) && (toadd.size() > 0)) {
				// weight votes by ordering
				Double currvote = basevote;
				for(int kid = 0; kid < toadd.size(); kid++) {
					TileKey key = toadd.get(kid);
					//System.out.println("key: "+key+",vote: "+currvote);
					Double count = predictions.get(key);
					if(count == null) {
						predictions.put(key,currvote);
					} else {
						//System.out.println("key: "+key+",vote: "+currvote);
						predictions.put(key,count+currvote);
					}
					currvote /= 2;
				}
			}
		}
		List<TileVote> votes = new ArrayList<TileVote>();
		for(TileKey candidate : predictions.keySet()) {
			votes.add(new TileVote(candidate,predictions.get(candidate)));
		}
		Collections.sort(votes,Collections.reverseOrder()); // sort by votes
		int end = defaultpredictions;
		if(end > votes.size()) {
			end = votes.size();
		}
		votes = votes.subList(0,end); // truncate to get the final list
		List<TileKey> output = new ArrayList<TileKey>();
		for(TileVote finalvote : votes) {
			output.add(finalvote.key);
			//System.out.println("predicted: '"+finalvote.key+"' with votes: '"+finalvote.vote+"'");
		}
		return output;
	}
	
	public static void insertPredictions(List<TileKey> predictions) {
		//System.out.print("predictions to insert:");
		//for(TileKey key : predictions) {
		//	System.out.print(key+" ");
		//}
		//System.out.println();
		if(predictions != null) {
			for(TileKey key: predictions) { // insert the predictions into the cache
				if(!membuf.peek(key)) { // not in memory
					NiceTile tile = diskbuf.getTile(key);
					if(tile == null) { // not on disk
						// get from database
						tile = scidbapi.getNiceTile(key);
						//insert in disk cache
						diskbuf.insertTile(tile);
					} else { // found it on disk
						// update timestamp
						diskbuf.touchTile(key);
					}
					//insert in mem cache
					membuf.insertTile(tile);
				} else { // found it in memory
					// update timestamp
					membuf.touchTile(key);
				}
			}
		}
		//System.out.print("tiles in cache:");
		//for(TileKey key : membuf.getAllTileKeys()) {
		//	System.out.print(key+" ");
		//}
		//System.out.println();
	}
	
	public static void setupServer(int port) throws Exception {
		server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/gettile");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new FetchTileServlet()), "/*");
		server.start();
	}

	public static void main(String[] args) throws Exception {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		int port = defaultport;
		
		if(args.length == 1) { // setup MainThread with the given port
			port = Integer.parseInt(args[0]);
		}
		
		// initialize cache managers
		membuf = new MemoryNiceTileBuffer();
		//diskbuf = new DiskNiceTileBuffer(DBInterface.cache_root_dir,DBInterface.hashed_query,DBInterface.threshold);
		diskbuf = new DiskNiceTileBuffer(DBInterface.nice_tile_cache_dir,DBInterface.hashed_query,DBInterface.threshold);
		scidbapi = new ScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		hist = new TileHistoryQueue(histmax);
		
		//setup models for prediction
		setupModels();
		trainModels();
		
		//start the server
		setupServer(port);
	}
	
	private static class TileVote implements Comparable<TileVote>{
		TileKey key;
		double vote;
		
		public TileVote(TileKey key, double vote) {
			this.key = key;
			this.vote = vote;
		}
		
		@Override
		public int compareTo(TileVote other) {
			double diff = this.vote - other.vote;
			if(diff < 0) {
				return -1;
			} else if(diff > 0) {
				return 1;
			} else {
				return 0;
			}
		}
	}
	
	public static void update_users(String[] userstrs) {
		int[] argusers = new int[userstrs.length];
		for(int i = 0; i < userstrs.length; i++) {
			System.out.println("userstrs["+i+"] = '"+userstrs[i]+"'");
			argusers[i] = Integer.parseInt(userstrs[i]);
		}
		user_ids = argusers;
	}
	
	public static void update_model_labels(String[] modelstrs) {
		modellabels = new Model[modelstrs.length];
		historylengths = new int[modelstrs.length];
		for(int i = 0; i < modelstrs.length; i++) {
			System.out.println("modelstrs["+i+"] = '"+modelstrs[i]+"'");
			historylengths[i] = defaulthistorylength;
			if(modelstrs[i].contains("ngram")) {
				modellabels[i] = Model.NGRAM;
				historylengths[i] = Integer.parseInt(modelstrs[i].substring(5));
			} else if(modelstrs[i].equals("random")) {
				modellabels[i] = Model.RANDOM;
			} else if(modelstrs[i].contains("hotspot")) {
				modellabels[i] = Model.HOTSPOT;
				if(modelstrs[i].length() > 7) {
					historylengths[i] = Integer.parseInt(modelstrs[i].substring(8));
				}
			} else if(modelstrs[i].contains("momentum")) {
				modellabels[i] = Model.MOMENTUM;
				if(modelstrs[i].length() > 8) {
					historylengths[i] = Integer.parseInt(modelstrs[i].substring(8));
				}
			} else if(modelstrs[i].equals("normal")) {
				modellabels[i] = Model.NORMAL;
			} else if(modelstrs[i].equals("histogram")) {
				modellabels[i] = Model.HISTOGRAM;
			} else if(modelstrs[i].equals("fhistogram")) {
				modellabels[i] = Model.FHISTOGRAM;
			} else if(modelstrs[i].equals("sift")) {
				modellabels[i] = Model.SIFT;
			} else if (modelstrs[i].equals("dsift")) {
				modellabels[i] = Model.DSIFT;
			}
		}
	}
	
	public static void update_allocations(String[] allocations) {
		int required = 0;
		allocatedStorage = new int[allocations.length];
		for(int i = 0; i < allocations.length; i++) {
			System.out.println("allocations["+i+"] = '"+allocations[i]+"'");
			allocatedStorage[i] = Integer.parseInt(allocations[i]);
			required += allocatedStorage[i];
		}
		membuf.clear();
		membuf.setStorageMax(required);
	}
	
	public static void reset(String[] userstrs, String[] modelstrs, String[] predictions) throws Exception {
		update_users(userstrs);
		update_model_labels(modelstrs);
		update_allocations(predictions);
		//defaultpredictions = Integer.parseInt(predictions);
		//System.out.println("predictions: "+defaultpredictions);
		
		//reset accuracy
		cache_hits = 0;
		total_requests = 0;
		hitslist = new ArrayList<String>();
		
		// reinitialize caches and user history
		membuf.clear();
		//don't reset this, it takes forever
		//diskbuf = new DiskNiceTileBuffer(DBInterface.cache_root_dir,DBInterface.hashed_query,DBInterface.threshold);
		hist.clear();
		
		setupModels();
		trainModels();
	}
	
	/**
	 * Java requires a serial version ID for the class.
	 */
	public static class FetchTileServlet extends HttpServlet {

		private static final long serialVersionUID = 6537664694070363096L;
		private static final String greeting = "Hello World";
		private static final String done = "done";
		private static final String error = "error";

		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException {
			
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			// get fetch parameters
			//String hashed_query = request.getParameter("hashed_query");
			String reset = request.getParameter("reset");
			if(reset != null) {
				System.out.println("reset");
				//reset models for prediction
				String useridstr = request.getParameter("user_ids");
				String modelstr = request.getParameter("models");
				String predictions = request.getParameter("predictions");
				try {
					reset(useridstr.split("_"),modelstr.split("_"), predictions.split("_"));
					response.getWriter().println(done);
				} catch (Exception e) {
					System.out.println("error resetting");
					e.printStackTrace();
				}
				//response.getWriter().println();
				return;
			}
			
			String getfullaccuracy = request.getParameter("fullaccuracy");
			if(getfullaccuracy !=null) {
				if(hitslist.size() == 0) {
					response.getWriter().println("[]");
					return;
				}
				String res = hitslist.get(0);
				for(int i = 1; i < hitslist.size(); i++) {
					res = res + ","+hitslist.get(i);
				}
				response.getWriter().println(res);
				return;
			}
			
			String getaccuracy = request.getParameter("accuracy");
			if(getaccuracy != null) {
				double accuracy = getAccuracy();
				response.getWriter().println(accuracy);
				return;
			}
			
			String zoom = request.getParameter("zoom");
			String tile_id = request.getParameter("tile_id");
			String threshold = request.getParameter("threshold");
			//System.out.println("hashed query: " + hashed_query);
			//System.out.println("zoom: " + zoom);
			//System.out.println("tile id: " + tile_id);
			//System.out.println("threshold: " + threshold);
			NiceTile t = null;
			try {
				t = fetchTile(tile_id,zoom,threshold);
			} catch (Exception e) {
				System.out.println("error occured while fetching tile");
				response.getWriter().println(error);
				e.printStackTrace();
				return;
			}
			if(t == null) {
				response.getWriter().println(greeting);
			} else {
				response.getWriter().println(NiceTilePacker.packData(t.data));
			}
		}
		
		protected double getAccuracy() {
			return (1.0 * cache_hits / total_requests);
		}
		
		// fetches tiles from
		protected NiceTile fetchTile(String tile_id, String zoom, String threshold) throws Exception {
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			int[] id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			TileKey key = new TileKey(id,z);
			//List<TileKey> predictions = null;
			
			System.out.println("last key: "+hist.getLast());
			System.out.println("key to predict: "+key);
			//System.out.println("history length: " + hist.getHistoryLength());
			//System.out.println("history:");
			//System.out.println(hist);

			// get predictions for next request
			//long pstart = System.currentTimeMillis();
			//predictions = getPredictions();
			//insertPredictions(predictions);
			doPredictions();
			//long pend = System.currentTimeMillis();
			//System.out.println("time to insert predictions: " + ((pend - pstart)/1000)+"s");
			
			boolean found = false;
			//long start = System.currentTimeMillis();
			NiceTile t = membuf.getTile(key);
			if(t == null) { // not cached
				//System.out.println("tile is not in mem-based cache");
				// go find the tile on disk
				t = diskbuf.getTile(key);
				if(t == null) { // not in memory
					//System.out.println("tile is not in disk-based cache. computing...");
					t = scidbapi.getNiceTile(key); 
					diskbuf.insertTile(t);
				} else { // found on disk
					//System.out.println("found tile in disk-based cache");
					//System.out.println("data size: " + t.getDataSize());
					// update timestamp
					diskbuf.touchTile(key);
				}
				// put the tile in the cache
				membuf.insertTile(t);
			} else { // found in memory
				cache_hits++;
				//System.out.println("found tile in mem-based cache");
				//System.out.println("data size: " + t.getDataSize());
				// update timestamp
				membuf.touchTile(key);
				found = true;
			}
			total_requests++;
			hist.addRecord(t);
			//long end = System.currentTimeMillis();
			//System.out.println("time to retrieve requested tile: " + ((end - start)/1000)+"s");
			if(found) {
				//System.out.println("hit in cache for tile "+key);
				hitslist.add("hit");
			} else {
				//System.out.println("miss in cache for tile "+key);
				hitslist.add("miss");
			}
			//System.out.println("current accuracy: "+ (1.0 * cache_hits / total_requests));
			//System.out.println("cache size: "+membuf.tileCount()+" tiles");
			return t;
		}

	}

}
