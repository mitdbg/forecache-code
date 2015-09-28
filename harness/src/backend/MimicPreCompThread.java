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

import backend.disk.BigDawgTileInterface;
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

public class MimicPreCompThread {
	public static BigDawgTileInterface bigdawgapi;
	
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
		
		bigdawgapi = new BigDawgTileInterface();
		
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
		
		protected void doFetch(HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			
			int k = Integer.parseInt(request.getParameter("k"));
			int aggWindow = Integer.parseInt(request.getParameter("aggWindow"));
			int tile_id = Integer.parseInt(request.getParameter("tile_id"));
			String recordName = request.getParameter("recordName");
			//System.out.println("k: " + k);
			//System.out.println("aggWindow: " + aggWindow);
			//System.out.println("tile id: " + tile_id);
			//System.out.println("recordName: " + recordName);
			String t = null;
			try {
				long ns = System.currentTimeMillis();
				t = fetchTile(k,aggWindow,recordName,tile_id);
				BigDawgTileInterface.BigDawgResponseObject obj = bigdawgapi.parseBDJsonObject(t);
				System.out.println("total records in tile: "+obj.tuples.length);
				System.out.println(t);
				// send the response
				long s = System.currentTimeMillis();
				long e = System.currentTimeMillis();
				response.getWriter().write(t);
				long e2 = System.currentTimeMillis();
				String report= (s-ns)+","+(e-s)+","+(e2-e)+","+t.length();
				System.out.println(report);
				//log.write(report);
				//log.newLine();
				//log.flush();
				
			} catch (Exception e) {
				System.err.println("error occured while fetching tile");
				e.printStackTrace();
				response.getWriter().println(error);
			}
		}
		
		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException {
			
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			
			String fetch = request.getParameter("fetch");
			if(fetch != null) {
				//System.out.println("doing tile fetch.");
				doFetch(request,response);
				return;
			}
		}
		
		protected String fetchTile(int k, int aggWindow, String recordName,
				int tile_id) throws Exception {
			String response = bigdawgapi.getRawTile(tile_id, k, recordName, aggWindow);
			return response;
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
