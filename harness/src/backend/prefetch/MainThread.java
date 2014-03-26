package backend.prefetch;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import backend.precompute.DiskTileBuffer;
import backend.util.Tile;
import backend.util.TileKey;
import utils.DBInterface;
import utils.UtilityFunctions;

public class MainThread {
	public static MemoryTileBuffer membuf;
	public static DiskTileBuffer diskbuf;
	public static int histmax = 10;
	public static TileHistoryQueue hist;
	
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
		hist = new TileHistoryQueue(histmax);
		
		//start the server
		setupServer();
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
			fetchTile(tile_id,zoom,threshold);
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(greeting);
		}
		
		// fetches tiles from
		protected void fetchTile(String tile_id, String zoom, String threshold) {
			String reverse = UtilityFunctions.unurlify(tile_id); // undo urlify
			List<Integer> id = UtilityFunctions.parseTileIdInteger(reverse);
			int z = Integer.parseInt(zoom);
			TileKey key = new TileKey(id,z);
			long start = System.currentTimeMillis();
			Tile t = membuf.getTile(key);
			if(t == null) { // not cached
				System.out.println("tile is not in mem-based cache");
				// go find the tile on disk
				t = diskbuf.getTile(key);
				if(t == null) {
					System.out.println("tile is not in disk-based cache");
				} else {
					System.out.println("found tile in disk-based cache");
					System.out.println("data size: " + t.getDataSize());
					// put the tile in the cache
					membuf.insertTile(t);
					hist.addRecord(t);
				}
			} else {
				System.out.println("found tile in mem-based cache");
				System.out.println("data size: " + t.getDataSize());
				// update timestamp
				membuf.insertTile(t);
				hist.addRecord(t);
			}
			System.out.println("history length: " + hist.getHistoryLength());
			long end = System.currentTimeMillis();
			System.out.println("time to retrieve in seconds: " + ((start - end)/1000));
		}

	}

}
