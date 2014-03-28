package backend.prefetch;

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

import backend.precompute.DiskTileBuffer;
import backend.precompute.ScidbTileInterface;
import backend.prefetch.similarity.MarkovDirectionalModel;
import backend.prefetch.similarity.RandomDirectionalModel;
import backend.prefetch.similarity.TrainModels;
import backend.util.Model;
import backend.util.ParamsMap;
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
	
	//accuracy
	public static int total_requests = 0;
	public static int cache_hits = 0;
	
	// General Model variables
	public static final Model[] modellabels = {Model.MARKOV, Model.RANDOM};
	public static final String taskname = "task1";
	public static final int[] user_ids = {27,28};
	public static final int defaultpredictions = 9;
	
	// global model objects
	public static MarkovDirectionalModel mdm;
	public static RandomDirectionalModel rdm;
	
	public static void setupModels() {
		for(int i = 0; i < modellabels.length; i++) {
			Model label = modellabels[i];
			switch(label) {
				case MARKOV: mdm = new MarkovDirectionalModel(MarkovDirectionalModel.defaultlen,hist);
				break;
				case RANDOM: rdm = new RandomDirectionalModel(hist);
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
				default://do nothing
			}
		}
	}
	
	public static List<TileKey> getPredictions() {
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
				default: toadd = null;
			}
			// count votes per prediction scheme
			if((toadd != null) && (toadd.size() > 0)) {
				// weight votes by ordering
				Double currvote = basevote*toadd.size();
				for(int kid = 0; kid < toadd.size(); kid++) {
					TileKey key = toadd.get(kid);
					System.out.println("key: "+key+",vote: "+currvote);
					Double count = predictions.get(key);
					if(count == null) {
						predictions.put(key,currvote);
					} else {
						//System.out.println("key: "+key+",vote: "+currvote);
						predictions.put(key,count+currvote);
					}
					currvote -= basevote;
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
			System.out.println("predicted: '"+finalvote.key+"' with votes: '"+finalvote.vote+"'");
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
		Server server = new Server(8080);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/gettile");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new FetchTileServlet()), "/*");
		server.start();
	}

	public static void main(String[] args) throws Exception {
		// initialize cache managers
		membuf = new MemoryTileBuffer();
		diskbuf = new DiskTileBuffer(DBInterface.cache_root_dir,DBInterface.hashed_query,DBInterface.threshold);
		scidbapi = new ScidbTileInterface(ParamsMap.defaultparamsfile,ParamsMap.defualtdelim);
		hist = new TileHistoryQueue(histmax);
		
		//start the server
		setupServer();
		
		//setup models for prediction
		setupModels();
		trainModels();
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
	
	/**
	 * Java requires a serial version ID for the class.
	 * Has something to do with it being serializable?
	 */
	public static class FetchTileServlet extends HttpServlet {

		private static final long serialVersionUID = 6537664694070363096L;
		private static final String greeting = "Hello World";

		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException {
			
			// get fetch parameters
			//String hashed_query = request.getParameter("hashed_query");
			String zoom = request.getParameter("zoom");
			String tile_id = request.getParameter("tile_id");
			String threshold = request.getParameter("threshold");
			//System.out.println("hashed query: " + hashed_query);
			System.out.println("zoom: " + zoom);
			System.out.println("tile id: " + tile_id);
			System.out.println("threshold: " + threshold);
			Tile t = fetchTile(tile_id,zoom,threshold);
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			if(t == null) {
				response.getWriter().println(greeting);
			} else {
				response.getWriter().println(t);
			}
		}
		
		// fetches tiles from
		protected Tile fetchTile(String tile_id, String zoom, String threshold) {
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			List<Integer> id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			TileKey key = new TileKey(id,z);
			List<TileKey> predictions = null;
			
			long start = System.currentTimeMillis();
			Tile t = membuf.getTile(key);
			if(t == null) { // not cached
				System.out.println("tile is not in mem-based cache");
				// go find the tile on disk
				t = diskbuf.getTile(key);
				if(t == null) { // not in memory
					System.out.println("tile is not in disk-based cache. computing...");
					t = scidbapi.getTile(key);
					diskbuf.insertTile(t);
				} else { // found on disk
					System.out.println("found tile in disk-based cache");
					System.out.println("data size: " + t.getDataSize());
					// update timestamp
					diskbuf.touchTile(key);
				}
				// put the tile in the cache
				membuf.insertTile(t);
			} else { // found in memory
				cache_hits++;
				System.out.println("found tile in mem-based cache");
				System.out.println("data size: " + t.getDataSize());
				// update timestamp
				membuf.touchTile(key);
			}
			total_requests++;
			hist.addRecord(t);
			//System.out.println("history length: " + hist.getHistoryLength());
			System.out.println("history:");
			System.out.println(hist);
			long end = System.currentTimeMillis();
			// get predictions for next request
			predictions = getPredictions();
			insertPredictions(predictions);
			long end2 = System.currentTimeMillis();
			System.out.println("time to retrieve requested tile: " + ((end - start)/1000)+"s");
			System.out.println("time to insert predictions: " + ((end2 - end)/1000)+"s");
			System.out.println("current accuracy: "+ (1.0 * cache_hits / total_requests));
			return t;
		}

	}

}
