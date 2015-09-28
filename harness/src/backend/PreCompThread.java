package backend;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.opencv.core.Core;

import configurations.BigDawgConfig;
import configurations.Config;
import configurations.DBConnector;
import configurations.ModisConfig;
import configurations.VMConfig;

import frontend.CacheLevel;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.NiceTilePacker;
import backend.disk.OldScidbTileInterface;
import backend.disk.PreCompNiceTileBuffer;
import backend.disk.PreCompNiceTileLruBuffer;
import backend.disk.ScidbTileInterface;
import backend.disk.VerticaTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryNiceTileLruBuffer;
import backend.prediction.TileHistoryQueue;
import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.SignatureMap;
import backend.util.TileKey;
import utils.DBInterface;
import utils.UtilityFunctions;

public class PreCompThread {
	//public static OldScidbTileInterface OldScidbapi;
	public static ScidbTileInterface scidbapi;
	public static VerticaTileInterface verticaapi;
	
	public static boolean usemem = false;
	public static boolean usepc = false;
	public static DiskNiceTileBuffer diskbuf;
	
	public static PredictionManager pcManager;
	public static PredictionManager memManager;
	public static TileHistoryQueue hist;
	public static SignatureMap sigMap;
	
	//server
	public static Server server;
	public static BufferedWriter log;
	public static int defaultport = 8080;
	
	// General prediction variables
	public static int histmax = 10;
	public static int deflmbuflen = 0; // default is don't use lru cache
	public static int neighborhood = 1; // default neighborhood from which to pick candidates
	public static Config conf;


	public static void setupServer(int port) throws Exception {
		server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/gettile");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new FetchTileServlet()), "/*");
		server.start();
	}
	
	public static void initializeCacheManagers(PredictionManager manager,
			NiceTileBuffer buf, NiceTileBuffer lrubuf) throws Exception {		
		// initialize cache managers
		manager.buf = buf;
		manager.diskbuf = diskbuf; // just used for convenience
		manager.dbapi = scidbapi;
		manager.hist = hist;
		manager.lmbuf = lrubuf; // tracks the user's last x moves
		
		// load pre-computed signature map
		manager.sigMap  = sigMap;
		
		//setup models for prediction
		//manager.setupModels();
		//manager.trainModels();
	}

	public static void main(String[] args) throws Exception {
		// tell java where opencv is
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//set configurations
		conf = new VMConfig();
		// conf = new BigDawgConfig();
		// conf = new ModisConfig();
		conf.setConfig();
		
		int lmbuflen = deflmbuflen;
		int port = defaultport;
		
		if(args.length >= 1) { // setup MainThread with the given port
			port = Integer.parseInt(args[0]);
		}
		
		if(args.length >= 2) {
			System.out.println("lmbuflen: "+args[1]);
			lmbuflen = Integer.parseInt(args[1]);
		}
		
		if(args.length == 3) {
			neighborhood = Integer.parseInt(args[2]);
			System.out.println("neighborhood: "+neighborhood);
		}
		
		hist = new TileHistoryQueue(histmax);
		diskbuf = new DiskNiceTileBuffer(DBInterface.nice_tile_cache_dir,
				DBInterface.hashed_query,DBInterface.threshold);
		scidbapi = new ScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		scidbapi.simulation_buffer = diskbuf; // so we don't have to query the dbms
		sigMap  = SignatureMap.getFromFile(BuildSignaturesOffline.defaultFilename);
		
		//initialize MM cache manager
		memManager = new PredictionManager();
		MemoryNiceTileBuffer membuf = new MemoryNiceTileBuffer();
		 // tracks the user's last x moves
		MemoryNiceTileLruBuffer memlrubuf = new MemoryNiceTileLruBuffer(lmbuflen);
		initializeCacheManagers(memManager,membuf,memlrubuf);
		
		// initialize pre-comp cache manager
		pcManager = new PredictionManager();
		
		PreCompNiceTileBuffer pcbuf;
		PreCompNiceTileLruBuffer pclrubuf;
		if(conf.getDB() == DBConnector.BIGDAWG) {
			pcbuf = new PreCompNiceTileBuffer(scidbapi);
			pclrubuf = new PreCompNiceTileLruBuffer(scidbapi,
				lmbuflen,pcbuf.isBuilt);
		} else {
			pcbuf = new PreCompNiceTileBuffer(scidbapi);
			pclrubuf = new PreCompNiceTileLruBuffer(scidbapi,
				lmbuflen,pcbuf.isBuilt);
		}
		initializeCacheManagers(pcManager,pcbuf,pclrubuf);
		
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
			return (!usemem || memManager.isReady()) && (!usepc || pcManager.isReady());
		}
		
		protected void cancelPredictions() throws Exception {
			if(usepc) pcManager.cancelPredictorJob();
			if(usemem) memManager.cancelPredictorJob();
		}
		
		protected void makePredictions() {
			if(usepc) pcManager.runPredictor(executorService);
			if(usemem) memManager.runPredictor(executorService);
		}
		
		protected void updatePredictorLruBuffers(NiceTile tile) {
			if(usemem) memManager.lmbuf.insertTile(tile);
			if(usepc) pcManager.lmbuf.insertTile(tile);
		}
		
		protected void updateAccuracy(TileKey id) {
			if(usemem) memManager.updateAccuracy(id);
			if(usepc) pcManager.updateAccuracy(id);
		}
		
		protected void doMemReset(String useridstr,String modelstr,String predictions,
				String nstr,String usePhases) throws Exception {
			if(usemem) {
				System.out.println("Doing mem reset...");
				memManager.reset(useridstr.split("_"),modelstr.split("_"),
						predictions.split("_"), nstr,
						(usePhases != null) && usePhases.equals("true"));
			} else {
				memManager.clear();
			}
		}
		
		protected void doPcReset(String useridstr,String modelstr,String predictions,
				String nstr,String usePhases) throws Exception {
			if(usepc) {
				System.out.println("Doing disk reset...");
				pcManager.reset(useridstr.split("_"),modelstr.split("_"),
						predictions.split("_"), nstr,
						(usePhases != null) && usePhases.equals("true"));
			} else {
				pcManager.clear();
			}
		}
		
		protected void doSetCacheLevels(HttpServletRequest request,
				HttpServletResponse response) {
			String memp = request.getParameter("usemem");
			String pcp = request.getParameter("usepc");
			usemem = memp != null;
			usepc = pcp != null;
			System.out.println("setting cache levels... usemem="+usemem+", usepc="+usepc);
			if(!usemem) memManager.clear();
			if(!usepc) pcManager.clear();
			try {
				response.getWriter().println(done);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		protected void doReset(HttpServletRequest request,
				HttpServletResponse response) {
			System.out.println("reset");
			//reset models for prediction
			String level = request.getParameter("level");
			String useridstr = request.getParameter("user_ids");
			String modelstr = request.getParameter("models");
			String predictions = request.getParameter("predictions");
			String usePhases = request.getParameter("usephases");
			String nstr = request.getParameter("neighborhood");
			try {
				if(level.equals(CacheLevel.SERVERMM.toString())) {
					doMemReset(useridstr,modelstr,predictions, nstr,usePhases);
				}
				else if (level.equals(CacheLevel.SERVERDISK.toString())) {
					doPcReset(useridstr,modelstr,predictions, nstr,usePhases);
				}
				hist.clear();
				response.getWriter().println(done);
			} catch (Exception e) {
				System.out.println("error resetting");
				e.printStackTrace();
			}
			//response.getWriter().println();
		}
		
		protected void doGetFullAccuracy(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			CacheLevel level = UtilityFunctions.getCacheLevel(request.getParameter("level"));
			String res = null;
			switch(level) {
			case SERVERMM: res = memManager.getFullAccuracy();
			break;
			case SERVERDISK: res = pcManager.getFullAccuracy();
			break;
			}
			if(res != null) response.getWriter().println(res);
		}
		
		protected void doGetAccuracy(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			CacheLevel level = UtilityFunctions.getCacheLevel(request.getParameter("level"));
			double accuracy = -1;
			switch(level) {
			case SERVERMM: accuracy = memManager.getAccuracy();
			break;
			case SERVERDISK: accuracy = pcManager.getAccuracy();
			break;
			}
			response.getWriter().println(accuracy);
			return;
		}
		
		protected void doFetch(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			
			String zoom = request.getParameter("zoom");
			String tile_id = request.getParameter("tile_id");
			String threshold = request.getParameter("threshold");
			//System.out.println("hashed query: " + hashed_query);
			//System.out.println("zoom: " + zoom);
			//System.out.println("tile id: " + tile_id);
			//System.out.println("threshold: " + threshold);
			NiceTile t = null;
			try {
				long ns = System.currentTimeMillis();
				t = fetchTile(tile_id,zoom,threshold);
				// send the response
				long s = System.currentTimeMillis();
				byte[] toSend = NiceTilePacker.packNiceTile(t);
				long e = System.currentTimeMillis();
				response.getOutputStream().write(toSend,0,toSend.length);
				long e2 = System.currentTimeMillis();
				String report= (s-ns)+","+(e-s)+","+(e2-e)+","+toSend.length;
				System.out.println(report);
				//log.write(report);
				//log.newLine();
				//log.flush();

				hist.addRecord(t);
				updatePredictorLruBuffers(t);
				
				makePredictions();
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
			String threshold = request.getParameter("threshold");
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			int[] id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			TileKey key = new TileKey(id,z);
			
			updateAccuracy(key); // just see if it's in the cache
			
			// get the tile, so we can put it in the history
			// this should not impact the latency numbers
			NiceTile t = diskbuf.getTile(key);
			if(t == null) { // check dbms
				t = new NiceTile();
				t.id = key;
				scidbapi.getStoredTile(DBInterface.arrayname, t);
			}
			hist.addRecord(t); // keep track
			
			// make some new predictions, just in case
			makePredictions();
		}
		
		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException {
			
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			
			String ready = request.getParameter("ready");
			if(ready != null) {
				response.getWriter().println(isReady());
				return;
			}
			
			String cachelevels = request.getParameter("cachelevels");
			if(cachelevels != null) {
				doSetCacheLevels(request,response);
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
		
		protected NiceTile fetchTile(String tile_id, String zoom,
				String threshold) throws InterruptedException {
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			int[] id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			TileKey key = new TileKey(id,z);
			
			//System.out.println("tile to predict: "+key);
			//System.out.println("last request: "+hist.getLast());
			
			updateAccuracy(key); // tracks hits/misses
			
			//boolean found = false;
			NiceTile t = null;
			
			if(!usemem && !usepc) {
				return diskbuf.getTile(key);
			}
			
			if (usemem) t = memManager.lmbuf.getTile(key); // check lru cache
			if(t == null) { // not in user's last x moves. check mem cache
				if(usemem) t = memManager.buf.getTile(key);
				if(t == null) { // not cached, get it from disk in DBMS
					if(usepc) pcManager.lmbuf.getTile(key);
					if(t == null) {
						if(usepc) {
							t = pcManager.buf.getTile(key);
							if(t == null) {
								t = new NiceTile();
								t.id = key;
								scidbapi.getSimulatedBuildTile(DBInterface.arrayname, t);
							}
						} else {
							t = new NiceTile();
							t.id = key;
							scidbapi.getStoredTile(DBInterface.arrayname, t);
							//verticaapi.getStoredTile(DBInterface.arrayname, t);
						}
					}
				}
			}
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
