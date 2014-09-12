package backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryTileBuffer;
import backend.prediction.TileHistoryQueue;
import backend.prediction.TrainModels;
import backend.prediction.directional.HotspotDirectionalModel;
import backend.prediction.directional.MarkovDirectionalModel;
import backend.prediction.directional.MomentumDirectionalModel;
import backend.prediction.directional.RandomDirectionalModel;
import backend.prediction.signature.FilteredHistogramSignatureModel;
import backend.prediction.signature.HistogramSignatureModel;
import backend.prediction.signature.NormalSignatureModel;
import backend.util.Model;
import backend.util.Tile;
import backend.util.TileKey;
import utils.DBInterface;
import utils.UtilityFunctions;

public class MainThread {
	public static MemoryTileBuffer membuf;
	public static DiskTileBuffer diskbuf;
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
	public static Model[] modellabels = {Model.MOMENTUM};
	public static String taskname = "task1";
	public static int[] user_ids = {28};
	public static int defaultpredictions = 3;
	
	// global model objects
	public static MarkovDirectionalModel mdm;
	public static RandomDirectionalModel rdm;
	public static HotspotDirectionalModel hdm;
	public static MomentumDirectionalModel momdm;
	public static NormalSignatureModel nsm;
	public static HistogramSignatureModel hsm;
	public static FilteredHistogramSignatureModel fsm;
	
	public static void setupModels() {
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			switch(label) {
				case MARKOV: mdm = new MarkovDirectionalModel(MarkovDirectionalModel.defaultlen,hist);
				break;
				case RANDOM: rdm = new RandomDirectionalModel(hist);
				break;
				case HOTSPOT: hdm = new HotspotDirectionalModel(hist,HotspotDirectionalModel.defaulthotspotlen);
				break;
				case MOMENTUM: momdm = new MomentumDirectionalModel(hist);
				break;
				case NORMAL: nsm = new NormalSignatureModel(hist,membuf,diskbuf,scidbapi);
				break;
				case HISTOGRAM: hsm = new HistogramSignatureModel(hist,membuf,diskbuf,scidbapi);
				break;
				case FHISTOGRAM: fsm = new FilteredHistogramSignatureModel(hist,membuf,diskbuf,scidbapi);
				break;
				default://do nothing
			}
		}
	}
	
	public static void trainModels() {
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			switch(label) {
				case MARKOV: TrainModels.TrainMarkovDirectionalModel(user_ids, taskname, mdm);
				break;
				case HOTSPOT: TrainModels.TrainHotspotDirectionalModel(user_ids, taskname, hdm);
				break;
				default://do nothing
			}
		}
	}
	
	public static List<TileKey> getPredictions() throws Exception {
		Map<TileKey,Double> predictions = new HashMap<TileKey,Double>();
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			Double basevote = 1.0; // value of a vote from this model
			List<TileKey> toadd;
			switch(label) {
				case MARKOV: toadd = mdm.predictTiles(defaultpredictions);
				break;
				case RANDOM: toadd = rdm.predictTiles(defaultpredictions);
				break;
				case HOTSPOT: toadd = hdm.predictTiles(defaultpredictions);
				break;
				case MOMENTUM: toadd = momdm.predictTiles(defaultpredictions);
				break;
				case NORMAL: toadd = nsm.predictTiles(defaultpredictions);
				break;
				case HISTOGRAM: toadd = hsm.predictTiles(defaultpredictions);
				break;
				case FHISTOGRAM: toadd = fsm.predictTiles(defaultpredictions);
				break;
				default: toadd = null;
			}
			if(toadd != null){
				System.out.println("predictions for model "+label);
				for(TileKey k : toadd) {
					System.out.println(k);
				}
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
		if(predictions != null) {
			for(TileKey key: predictions) { // insert the predictions into the cache
				if(!membuf.peek(key)) { // not in memory
					Tile tile = diskbuf.getTile(key);
					if(tile == null) { // not on disk
						// get from database
						tile = scidbapi.getTile(key);
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
	}
	
	public static void setupServer() throws Exception {
		server = new Server(8080);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/gettile");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new FetchTileServlet()), "/*");
		server.start();
	}

	public static void main(String[] args) throws Exception {
		// get models to use
		if(args.length > 0) {
			String[] modelstrs = args[0].split(",");
			update_model_labels(modelstrs);
		}
		// get user ids to train on
		if(args.length > 1) {
			String[] userstrs = args[1].split(",");
			update_users(userstrs);
		}
		
		// get taskname
		if(args.length > 2) {
			taskname = args[2];
			System.out.println("taskname: "+taskname);
		}
		
		// get num predictions
		if(args.length > 3) {
			defaultpredictions = Integer.parseInt(args[3]);
			System.out.println("predictions: "+defaultpredictions);
		}
		
		// initialize cache managers
		membuf = new MemoryTileBuffer();
		diskbuf = new DiskTileBuffer(DBInterface.cache_root_dir,DBInterface.hashed_query,DBInterface.threshold);
		scidbapi = new ScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		hist = new TileHistoryQueue(histmax);
		
		//setup models for prediction
		setupModels();
		trainModels();
		
		//start the server
		setupServer();
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
		Model[] argmodels = new Model[modelstrs.length];
		for(int i = 0; i < modelstrs.length; i++) {
			System.out.println("modelstrs["+i+"] = '"+modelstrs[i]+"'");
			if(modelstrs[i].equals("markov")) {
				argmodels[i] = Model.MARKOV;
			} else if(modelstrs[i].equals("random")) {
				argmodels[i] = Model.RANDOM;
			} else if(modelstrs[i].equals("hotspot")) {
				argmodels[i] = Model.HOTSPOT;
			} else if(modelstrs[i].equals("momentum")) {
				argmodels[i] = Model.MOMENTUM;
			} else if(modelstrs[i].equals("normal")) {
				argmodels[i] = Model.NORMAL;
			} else if(modelstrs[i].equals("histogram")) {
				argmodels[i] = Model.HISTOGRAM;
			} else if(modelstrs[i].equals("fhistogram")) {
				argmodels[i] = Model.FHISTOGRAM;
			}
		}
		modellabels = argmodels;
	}
	
	public static void reset(String[] userstrs, String[] modelstrs, String predictions) throws Exception {
		update_users(userstrs);
		update_model_labels(modelstrs);
		defaultpredictions = Integer.parseInt(predictions);
		System.out.println("predictions: "+defaultpredictions);
		
		//reset accuracy
		cache_hits = 0;
		total_requests = 0;
		hitslist = new ArrayList<String>();
		
		// reinitialize caches and user history
		membuf = new MemoryTileBuffer(defaultpredictions);
		//don't reset this, it takes forever
		//diskbuf = new DiskTileBuffer(DBInterface.cache_root_dir,DBInterface.hashed_query,DBInterface.threshold);
		hist = new TileHistoryQueue(histmax);
		
		setupModels();
		trainModels();
	}
	
	/**
	 * Java requires a serial version ID for the class.
	 * Has something to do with it being serializable?
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
					reset(useridstr.split("_"),modelstr.split("_"), predictions);
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
			Tile t = null;
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
				response.getWriter().println(t.encodeData());
			}
		}
		
		protected double getAccuracy() {
			return (1.0 * cache_hits / total_requests);
		}
		
		// fetches tiles from
		protected Tile fetchTile(String tile_id, String zoom, String threshold) throws Exception {
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			List<Integer> id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			TileKey key = new TileKey(id,z);
			List<TileKey> predictions = null;
			
			//System.out.println("history length: " + hist.getHistoryLength());
			//System.out.println("history:");
			//System.out.println(hist);

			// get predictions for next request
			//long pstart = System.currentTimeMillis();
			predictions = getPredictions();
			insertPredictions(predictions);
			//long pend = System.currentTimeMillis();
			//System.out.println("time to insert predictions: " + ((pend - pstart)/1000)+"s");
			
			boolean found = false;
			//long start = System.currentTimeMillis();
			Tile t = membuf.getTile(key);
			if(t == null) { // not cached
				//System.out.println("tile is not in mem-based cache");
				// go find the tile on disk
				t = diskbuf.getTile(key);
				if(t == null) { // not in memory
					//System.out.println("tile is not in disk-based cache. computing...");
					t = scidbapi.getTile(key);
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
