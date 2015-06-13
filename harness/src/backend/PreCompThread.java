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

import frontend.CacheLevel;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.NiceTilePacker;
import backend.disk.OldScidbTileInterface;
import backend.disk.PreCompNiceTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.disk.VerticaTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.NiceTileLruBuffer;
import backend.prediction.TileHistoryQueue;
import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.SignatureMap;
import backend.util.TileKey;
import utils.DBInterface;
import utils.UtilityFunctions;

public class PreCompThread {
	public static OldScidbTileInterface OldScidbapi;
	public static ScidbTileInterface scidbapi;
	public static VerticaTileInterface verticaapi;
	
	public static boolean usemem = true;
	public static boolean usepc = false;
	public static MemoryNiceTileBuffer membuf;
	public static PreCompNiceTileBuffer pcbuf;
	public static DiskNiceTileBuffer diskbuf;
	public static NiceTileLruBuffer lmbuf;
	
	public static PredictionManager pcManager;
	public static PredictionManager memManager;
	public static TileHistoryQueue hist;
	public static SignatureMap sigMap;
	
	//server
	public static Server server;
	public static BufferedWriter log;
	
	//accuracy
	//public static int total_requests = 0;
	//public static int cache_hits = 0;
	//public static List<String> hitslist = new ArrayList<String>();
	
	// General Model variables
	public static int histmax = 10;
	public static int deflmbuflen = 0; // default is don't use lru cache
	public static int defaultport = 8080;
	public static int neighborhood = 1; // default neighborhood from which to pick candidates


	
	public static void setupServer(int port) throws Exception {
		server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/gettile");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new FetchTileServlet()), "/*");
		server.start();
	}
	
	public static void initializeCacheManagers(PredictionManager manager, NiceTileBuffer buf) throws Exception {		
		// initialize cache managers
		manager.buf = buf;
		manager.diskbuf = diskbuf; // just used for convenience
		manager.dbapi = scidbapi;
		manager.hist = hist;
		manager.lmbuf = lmbuf; // tracks the user's last x moves
		
		// load pre-computed signature map
		manager.sigMap  = sigMap;
		
		//setup models for prediction
		manager.setupModels();
		manager.trainModels();
	}

	public static void main(String[] args) throws Exception {
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
		
		if(args.length == 3) {
			neighborhood = Integer.parseInt(args[2]);
			System.out.println("neighborhood: "+neighborhood);
		}
		
		pcManager = new PredictionManager();
		memManager = new PredictionManager();
		
		// initialize cache managers
		scidbapi = new ScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		membuf = new MemoryNiceTileBuffer();
		diskbuf = new DiskNiceTileBuffer(DBInterface.nice_tile_cache_dir,DBInterface.hashed_query,DBInterface.threshold);
		pcbuf = new PreCompNiceTileBuffer(scidbapi);
		hist =new TileHistoryQueue(histmax);
		lmbuf = new NiceTileLruBuffer(lmbuflen); // tracks the user's last x moves
		
		sigMap  = SignatureMap.getFromFile(BuildSignaturesOffline.defaultFilename);
		
		initializeCacheManagers(pcManager,pcbuf);
		initializeCacheManagers(memManager,membuf);
		
		//logfile for timing results
		try {
			log = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("precomp_perflog.csv")));
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
			executorService = (ExecutorService) Executors.newCachedThreadPool();
		}
				
		protected boolean isReady() {
			return (!usemem || memManager.isReady()) && (!usepc || pcManager.isReady());
		}
		
		protected void updateAccuracy(TileKey id) {
			memManager.updateAccuracy(id);
			pcManager.updateAccuracy(id);
		}
		
		protected void doMemReset(String useridstr,String modelstr,String predictions,String usePhases) throws Exception {
			if(usemem) {
				System.out.println("Doing mem reset...");
				memManager.reset(useridstr.split("_"),modelstr.split("_"), predictions.split("_"),
						(usePhases != null) && usePhases.equals("true"));
			} else {
				memManager.clear();
			}
		}
		
		protected void doPcReset(String useridstr,String modelstr,String predictions,String usePhases) throws Exception {
			if(usepc) {
				System.out.println("Doing disk reset...");
				pcManager.reset(useridstr.split("_"),modelstr.split("_"), predictions.split("_"),
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
			try {
				if(level.equals(CacheLevel.SERVERMM.toString())) doMemReset(useridstr,modelstr,predictions,usePhases);
				else if (level.equals(CacheLevel.SERVERDISK.toString())) doPcReset(useridstr,modelstr,predictions,usePhases);

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
		
		protected void doComp(HttpServletRequest request,
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
				//t = fetchTile(tile_id,zoom,threshold);
				t = newFetchTile(tile_id,zoom,threshold);
				if(usepc) pcManager.runPredictor(executorService);
				if(usemem) memManager.runPredictor(executorService);
				// send the response
				//long s = System.currentTimeMillis();
				byte[] toSend = NiceTilePacker.packNiceTile(t);
				//long e = System.currentTimeMillis();
				response.getOutputStream().write(toSend,0,toSend.length);
				//long e2 = System.currentTimeMillis();
				//String report= (e-s)+","+(e2-e)+","+toSend.length;
				//System.out.println(report);
				//log.write(report);
				//log.newLine();
				//log.flush();
			} catch (Exception e) {
				response.getWriter().println(error);
				System.out.println("error occured while fetching tile");
				e.printStackTrace();
			}
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
				t = newFetchTile(tile_id,zoom,threshold);
				if(usepc) pcManager.runPredictor(executorService);
				if(usemem) memManager.runPredictor(executorService);
				// send the response
				//long s = System.currentTimeMillis();
				byte[] toSend = NiceTilePacker.packNiceTile(t);
				//long e = System.currentTimeMillis();
				response.getOutputStream().write(toSend,0,toSend.length);
				//long e2 = System.currentTimeMillis();
				//String report= (e-s)+","+(e2-e)+","+toSend.length;
				//System.out.println(report);
				//log.write(report);
				//log.newLine();
				//log.flush();
			} catch (Exception e) {
				response.getWriter().println(error);
				System.out.println("error occured while fetching tile");
				e.printStackTrace();
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
			
			// get the tile, so we can put it in the history
			NiceTile t = lmbuf.getTile(key); // check lru cache
			if(t == null) { // not in user's last x moves. check mem cache
				t = membuf.getTile(key);
				if(t == null) { // not cached, get it from disk in DBMS
					t = new NiceTile();
					t.id = key;
					scidbapi.getStoredTile(DBInterface.arrayname, t);
				}
			}
			
			hist.addRecord(t); // keep track
			lmbuf.insertTile(t);
			
			// pre-emptively cache, just in case
			if(usepc) pcManager.runPredictor(executorService);
			if(usemem) memManager.runPredictor(executorService);
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
		
		protected NiceTile newFetchTile(String tile_id, String zoom, String threshold) {
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			int[] id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			TileKey key = new TileKey(id,z);
			
			//System.out.println("tile to predict: "+key);
			//System.out.println("last request: "+hist.getLast());
			
			updateAccuracy(key);
			//boolean found = false;
			NiceTile t = null;
			if (usemem) t = lmbuf.getTile(key); // check lru cache
			if(t == null) { // not in user's last x moves. check mem cache
				if(usemem) t = membuf.getTile(key);
				if(t == null) { // not cached, get it from disk in DBMS
					if(usepc) {
						t = pcbuf.getTile(key);
					} else {
						t = new NiceTile();
						t.id = key;
						scidbapi.getStoredTile(DBInterface.arrayname, t);
						//verticaapi.getStoredTile(DBInterface.arrayname, t);
					}
					
					//membuf.insertTile(t);
				} else { // found in memory
					//cache_hits++;
					if(usemem) membuf.touchTile(key); // update timestamp
					//found = true;
				}
			} else { // found in lru cache
				//cache_hits++;
				//found = true;
			}
			//total_requests++;
			hist.addRecord(t);
			lmbuf.insertTile(t);
			/*
			if(found) {
				//System.out.println("hit in cache for tile "+key);
				hitslist.add("hit");
			} else {
				//System.out.println("miss in cache for tile "+key);
				hitslist.add("miss");
			}
			*/
			//System.out.println("current accuracy: "+ (1.0 * cache_hits / total_requests));
			//System.out.println("cache size: "+membuf.tileCount()+" tiles");
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
