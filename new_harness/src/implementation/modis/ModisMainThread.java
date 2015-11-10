package implementation.modis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.opencv.core.Core;

import configurations.BigDawgConfig;
import configurations.Config;
import configurations.ModisConfig;
import configurations.VMConfig;

import abstraction.enums.Model;
import abstraction.prediction.AllocationStrategyMap;
import abstraction.prediction.PredictionEngine;
import abstraction.query.NewTileInterface;
import abstraction.query.Scidb13_3IqueryTileInterface;
import abstraction.query.Scidb14_12IqueryTileInterface;
import abstraction.storage.MainMemoryTileBuffer;
import abstraction.storage.NiceTilePacker;
import abstraction.storage.TileBuffer;
import abstraction.storage.TileCacheManager;
import abstraction.storage.TileRetrievalHelper;
import abstraction.storage.eviction.LruPolicy;
import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.structures.SessionMetadata;
import abstraction.structures.TileStructure;
import abstraction.structures.View;
import abstraction.tile.ColumnBasedNiceTile;
import abstraction.util.DBInterface;
import abstraction.util.UtilityFunctions;

public class ModisMainThread {
	public static TileRetrievalHelper trh;
	public static SessionMetadata md;
	public static DefinedTileView dtv;
	
	public static PredictionEngine predictionEngine;
	public static TileCacheManager cacheManager;
	
	//server
	public static Server server;
	public static BufferedWriter log;
	public static int defaultport = 8080;
	
	// General prediction variables
	public static boolean doprefetch = false;
	public static int histmax = 10;
	public static int deflmbuflen = 0; // default is don't use lru cache
	public static int neighborhood = -1; // default neighborhood from which to pick candidates
	public static int baselen = -1;
	public static Config conf;

	public static String defaultSigmapFilename = "sigMap_k100.ser";

	public static void setupServer(int port) throws Exception {
		server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		// default servlet will look for requests to root path
		context.setContextPath("/");
		context.setResourceBase("web_content"); // web content is available in this folder
		System.out.println(context.getResourceBase());
		server.setHandler(context);
		
		// use the special FetchTileServlet to service fetch requests
		context.addServlet(new ServletHolder(new FetchTileServlet()), "/forecache/modis/fetch/*");
		// use a default servlet to get static stuff
		context.addServlet(new ServletHolder(new DefaultServlet()), "/*");
		server.start();
	}

	public static void main(String[] args) throws Exception {
		// tell java where opencv is
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		int lmbuflen = deflmbuflen;
		int port = defaultport;
		
		if(args.length >= 1) { // setup MainThread with the given port
			port = Integer.parseInt(args[0]);
		}
		
		if(args.length >= 2) {
			System.out.println("lmbuflen: "+args[1]);
			lmbuflen = Integer.parseInt(args[1]);
		}
		
		if(args.length >= 3) {
			neighborhood = Integer.parseInt(args[2]);
			System.out.println("neighborhood: "+neighborhood);
		}
		
		if(args.length >= 4) {
			//set configurations
			if(args[3].equals("vm")) {
				System.out.println("using vm config");
				conf = new VMConfig();
			} else if(args[3].equals("bigdawg")) {
				System.out.println("using big dawg config");
				conf = new BigDawgConfig();
			} else if (args[3].equals("modis")) {
				System.out.println("using modis config");
				conf = new ModisConfig();
			} else { // default
				System.out.println("config not recognized. using default config (vm) ....");
				conf = new VMConfig();
			}
			conf.setConfig();
		}
		
		if(args.length == 5) {
			doprefetch = args[4].equals("true");
		}
		
		// this code sets up the MODIS use case
		ModisViewFactory mvf = new ModisViewFactory();
		View v = mvf.getModisView();
		//TileStructure ts = OldModisTileStructureFactory.getDefaultModisTileStructure();
		TileStructure ts = OldModisTileStructureFactory.
				getModisTileStructure(OldModisTileStructureFactory.defaultAggregationWindows,
						new int[]{100,100}); // changing the tile parameters
		NewTileInterface nti = new Scidb13_3IqueryTileInterface();
		dtv = new DefinedTileView(v, ts, nti, defaultSigmapFilename,
				DBInterface.nice_tile_cache_dir);
		
		predictionEngine = new PredictionEngine();
		// buffer using LRU eviction policy
		TileBuffer buffer = new MainMemoryTileBuffer(0, new LruPolicy());
		cacheManager = new TileCacheManager(dtv, buffer);
		
		//logfile for timing results
		try {
			log = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("precomp_perflog.csv")));
		} catch (IOException e) {
		    System.out.println("Couldn't open logfile");
		    e.printStackTrace();
		    return;
		}
		
		//start the server
		setupServer(port);
	}
	
	/**
	 * Java requires a serial version ID for the class.
	 */
	public static class FetchTileServlet extends HttpServlet {

		private static final long serialVersionUID = 6537664694070363096L;
		private static final String done = "done";
		private static final String error = "error";
		private static ExecutorService executorService;
		
		public void init() {
			executorService = (ExecutorService) Executors.newFixedThreadPool(2);
		}
				
		protected boolean isReady() {
			return cacheManager.isReady(); // done prefetching?
		}
		
		protected void makePredictions() {
			List<NewTileKey> predictions = predictionEngine.getPredictions(md, dtv, neighborhood);
			cacheManager.insertPredictions(predictions);
		}
		
		protected void updateAccuracy(NewTileKey id) {
			cacheManager.updateAccuracy(id);
		}
		
		// assumes no prediction
		protected void doSimpleReset() {
			// new user session
			md = new SessionMetadata("", histmax, null);

			cacheManager.clear();
		}
		
		protected void doReset(String taskname, String useridstr,
				String modelstr,String predictions,
				String nstr, String baselenstr) throws Exception {
				System.out.println("Doing reset...");
				neighborhood = Integer.parseInt(nstr);
				baselen = Integer.parseInt(baselenstr);
				predictionEngine.reset(taskname,useridstr.split("_"),modelstr.split("_"), baselen);
				int[] allocations = SessionMetadata.parseAllocationStrings(predictions.split("_"));
				
				AllocationStrategyMap asm;
				
				// if we are using SIFT and NGRM only, use the hybrid allocation strategy instead
				if((predictionEngine.modellabels.length == 2) &&
						((predictionEngine.modellabels[0] == Model.SIFT) &&
						(predictionEngine.modellabels[1] == Model.NGRAM)) ||
						((predictionEngine.modellabels[0] == Model.NGRAM) &&
								(predictionEngine.modellabels[1] == Model.SIFT))) {
					int totalAllocations = allocations[0] + allocations[1];
					asm = ModisAllocationStrategyMapFactory.get2ModelHybridMap(totalAllocations);
				} else {
					// use the same allocations for every phase
					asm = ModisAllocationStrategyMapFactory.
							getBasicMap(predictionEngine.modellabels, allocations);
				}
				
				// new user session
				md = new SessionMetadata("", histmax, asm);

				cacheManager.clear();
		}
		
		protected void doReset(HttpServletRequest request,
				HttpServletResponse response) {
			System.out.println("reset");
			if(!doprefetch) { // no prefetching, ignore the prediction setup
				doSimpleReset();
				try {
					response.getWriter().print(done);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return;
			}
			//reset models for prediction
			//String level = request.getParameter("level");
			String useridstr = request.getParameter("user_ids");
			String modelstr = request.getParameter("models");
			String predictions = request.getParameter("predictions");
			//String usePhases = request.getParameter("usephases");
			String nstr = request.getParameter("neighborhood");
			String taskname = request.getParameter("taskname");
			String baselenstr = request.getParameter("histlen");
			try {
				doReset(taskname,useridstr,modelstr,predictions, nstr, baselenstr);
				response.getWriter().print(done);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//response.getWriter().println();
		}
		
		protected void doGetFullAccuracy(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			String res = cacheManager.getFullAccuracy();
			if(res != null) response.getWriter().print(res);
		}
		
		protected void doGetAccuracy(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			double accuracy =  cacheManager.getAccuracy();
			response.getWriter().print(accuracy);
		}
		
		protected void doGetTileStructure(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			String jsonstring = dtv.ts.toJson();
			response.getWriter().print(jsonstring);
		}
		
		protected void doGetView(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			String jsonstring = dtv.v.toJson();
			response.getWriter().print(jsonstring);
		}
		
		protected void doJsonFetch(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			
			String zoom = request.getParameter("zoom");
			String tile_id = request.getParameter("tile_id");
			//System.out.println("hashed query: " + hashed_query);
			//System.out.println("zoom: " + zoom);
			//System.out.println("tile id: " + tile_id);
			//System.out.println("threshold: " + threshold);
			ColumnBasedNiceTile t = null;
			try {
				long ns = System.currentTimeMillis();
				t = fetchTile(tile_id,zoom);
				// send the response
				long s = System.currentTimeMillis();
				String jsonstring = t.toJson();
				long e = System.currentTimeMillis();
				response.getWriter().print(jsonstring);
				long e2 = System.currentTimeMillis();
				String report= (s-ns)+","+(e-s)+","+(e2-e)+","+jsonstring.length();
				System.out.println(report);
				//log.write(report);
				//log.newLine();
				//log.flush();

				md.history.addRecord(t);
				
				if(doprefetch) {
					makePredictions();
				}
			} catch (Exception e) {
				System.err.println("error occured while fetching tile");
				e.printStackTrace();
				response.getWriter().println(error);
			}
		}
		
		protected void doFetch(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			
			String zoom = request.getParameter("zoom");
			String tile_id = request.getParameter("tile_id");
			//System.out.println("hashed query: " + hashed_query);
			//System.out.println("zoom: " + zoom);
			//System.out.println("tile id: " + tile_id);
			//System.out.println("threshold: " + threshold);
			ColumnBasedNiceTile t = null;
			try {
				long ns = System.currentTimeMillis();
				t = fetchTile(tile_id,zoom);
				// send the response
				long s = System.currentTimeMillis();
				byte[] toSend = NiceTilePacker.serializeTile(t);
				long e = System.currentTimeMillis();
				response.getOutputStream().write(toSend,0,toSend.length);
				long e2 = System.currentTimeMillis();
				String report= (s-ns)+","+(e-s)+","+(e2-e)+","+toSend.length;
				System.out.println(report);
				//log.write(report);
				//log.newLine();
				//log.flush();

				md.history.addRecord(t);
				
				if(doprefetch) {
					makePredictions();
				}
			} catch (Exception e) {
				System.err.println("error occured while fetching tile");
				e.printStackTrace();
				response.getWriter().println(error);
			}
		}
		
		protected void doAddHistory(HttpServletRequest request,
				HttpServletResponse response) {
			
			String zoom = request.getParameter("zoom");
			String tile_id = request.getParameter("tile_id");
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			int[] id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			NewTileKey key = new NewTileKey(id,z);
			
			updateAccuracy(key); // just see if it's in the cache
			
			// get the tile, so we can put it in the history
			// this should not impact the latency numbers
			ColumnBasedNiceTile t = cacheManager.getTileForMetadata(key);
			md.history.addRecord(t); // keep track
			
			if(doprefetch) {
				// make some new predictions, just in case
				makePredictions();
			}
		}
		
		// used to enable cross-domain AJAX calls
		private void fixHeaders(HttpServletResponse response) {
		    response.setHeader("Access-Control-Allow-Origin", "*");
		    response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, TRACE, OPTIONS");
		    response.setHeader("Access-Control-Allow-Headers", "Content-Type");
		    response.setHeader("Access-Control-Max-Age", "86400");
		    
		  //Tell the browser what requests we allow.
		    response.setHeader("Allow", "GET, HEAD, POST, TRACE, OPTIONS");
		    //System.out.println("fixing headers to allow CORS");
		}
		
		protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		    fixHeaders(response);
		}
		
		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException {
			
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			fixHeaders(response);
			
			String ready = request.getParameter("ready");
			if(ready != null) {
				response.getWriter().println(isReady());
				return;
			}
			
			String reset = request.getParameter("reset");
			if(reset != null) {
				doReset(request,response);
				return;
			}
			
			String getfullaccuracy = request.getParameter("fullaccuracy");
			if(getfullaccuracy !=null) {
				doGetFullAccuracy(request,response);
				return;
			}
			
			String getaccuracy = request.getParameter("accuracy");
			if(getaccuracy != null) {
				doGetAccuracy(request,response);
				return;
			}
			
			String getts = request.getParameter("getts");
			if(getts != null) {
				doGetTileStructure(request,response);
				return;
			}
			
			String getview = request.getParameter("getview");
			if(getview != null) {
				doGetView(request,response);
				return;
			}

			String json = request.getParameter("json");
			if(json != null) {
				//System.out.println("doing tile fetch.");
				doJsonFetch(request,response);
				return;
			}
			
			String fetch = request.getParameter("fetch");
			if(fetch != null) {
				//System.out.println("doing tile fetch.");
				doFetch(request,response);
				return;
			}
			
			String addhistory = request.getParameter("addhistory");
			if(addhistory != null) {
				//System.out.println("adding to history...");
				doAddHistory(request,response);
				return;
			}
		}
		
		protected ColumnBasedNiceTile fetchTile(String tile_id, String zoom) throws InterruptedException {
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			int[] id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			NewTileKey key = new NewTileKey(id,z);
			
			//System.out.println("tile to predict: "+key);
			//System.out.println("last request: "+hist.getLast());
			
			updateAccuracy(key); // tracks hits/misses
			
			//boolean found = false;
			ColumnBasedNiceTile t = null;
			
			//if (usemem) t = memManager.lmbuf.getTile(key); // check lru cache
			t = cacheManager.retrieveUserRequestedTile(key);
			return t;
		}
		
		public void destroy() {
			executorService.shutdown();
			try {
				log.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
