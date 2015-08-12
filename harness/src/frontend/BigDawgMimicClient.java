package frontend;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import configurations.BigDawgConfig;
import configurations.Config;
import configurations.ModisConfig;
import configurations.VMConfig;

import backend.disk.NiceTilePacker;
import backend.util.NiceTile;

import frontend.BigDawg.UserMap;
import frontend.BigDawg.MimicWorkQueue;

import utils.DBInterface;

public class BigDawgMimicClient {
	//server
	public static Server server;
	public static int defaultport = 10080;
	public static int backend_port = 8080;
	
	public static int[] defaultAggregationWindows = new int[]{1250,625,125,25,5,1};

	public static void main(String[] args) throws Exception {
		//set configurations
		Config conf;
		conf = new VMConfig();
		// conf = new BigDawgConfig();
		// conf = new ModisConfig();
		conf.setConfig();
		
		int port = Integer.parseInt(args[0]);
		backend_port = Integer.parseInt(args[1]);
		server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.setResourceBase("web_content");
		System.out.println(context.getResourceBase());
		server.setHandler(context);
		context.addServlet(new ServletHolder(new BigDawgFetchServlet()), "/forecache/mimic/fetch/*");
		//context.addServlet(new ServletHolder(new DefaultServlet()), "/*");
		server.start();
	}

	public static class BigDawgFetchServlet extends HttpServlet {
		private static final long serialVersionUID = -7335073175495796872L;
		private static final String wait = "wait";
		private static final String fail = "fail";
		private UserMap users;
		private MimicWorkQueue wq;

		public void init() {
			wq = new MimicWorkQueue(backend_port);
			wq.init();
			users = new UserMap();
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
		
		/*

		protected synchronized void fetchFirstTile(String user, HttpServletRequest request,
				HttpServletResponse response) throws IOException {

			//TODO: fix this
			//String tile_id = request.getParameter("tile_id");
			//String query = request.getParameter("query");
			String hashed_query = DBInterface.hashed_query;

			String jobid = wq.queueNewJob(user, "0_0_0", hashed_query);
			response.getWriter().println("\""+jobid+"\"");
		}
		*/

		protected synchronized void fetchTile(String user, HttpServletRequest request,
				HttpServletResponse response) throws IOException {

			//TODO: fix this
			int tile_id = Integer.parseInt(request.getParameter("tile_id"));
			int k = Integer.parseInt(request.getParameter("k"));
			int aggWindow = Integer.parseInt(request.getParameter("aggWindow"));
			String recordName = request.getParameter("recordName");

			String jobid = wq.queueNewJob(user, k, aggWindow, recordName, tile_id);
			response.getWriter().print(jobid);
		}

		protected synchronized void getJobResult(String user, HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			String jobid = request.getParameter("jobid");
			String data = wq.getJobResults(jobid);
			if(data == null) {
				if(wq.checkJobFail(jobid)) { // job failed
					response.getWriter().print(fail);
				} else { // not ready yet
					response.getWriter().print(wait);
				}
			} else { // results ready
				//response.getOutputStream().write(data,0,data.length);
				//NiceTile t = NiceTilePacker.unpackNiceTile(data);
				//String toSend = NiceTilePacker.makeJson(t);
				response.getWriter().print(data);
			}
		}

		protected String getNewUserId() {
			// generate a unique user id and send it back to the browser
			return UUID.randomUUID().toString();
		}
		
		protected void doPost(HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException {

			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			fixHeaders(response);
			String user = request.getParameter("user");

			String getUser = request.getParameter("guid");
			if(getUser != null) {
				System.out.println("received guid request");
				user = getNewUserId();
				response.getWriter().print(user);
			}
			users.put(user);

			/*
			String fft = request.getParameter("fft");
			if(fft != null) {
				fetchFirstTile(user,request,response);
			}
			*/

			String ft = request.getParameter("ft");
			if(ft != null) {
				System.out.println("received ft request");
				fetchTile(user,request,response);
			}

			String getResult = request.getParameter("gr");
			if(getResult != null) {
				System.out.println("received gr request.");
				getJobResult(user,request,response);
			}

			String reset = request.getParameter("reset");
			if(reset != null) {
				response.getWriter().print("");
			}

			users.prune();
		}

		public void destroy() {
			wq.shutdown();
		}
	}
}
