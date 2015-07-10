package frontend;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
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

import backend.disk.NiceTilePacker;
import backend.util.NiceTile;

import frontend.BigDawg.UserMap;
import frontend.BigDawg.WorkQueue;

import utils.DBInterface;

public class BigDawgClient {
	//server
	public static Server server;
	public static int defaultport = 10080;
	public static int backend_port = 8080;

	public static void main(String[] args) throws Exception {
		int port = Integer.parseInt(args[0]);
		backend_port = Integer.parseInt(args[1]);
		server = new Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.setResourceBase("web_content");
		System.out.println(context.getResourceBase());
		server.setHandler(context);
		context.addServlet(new ServletHolder(new BigDawgFetchServlet()), "/scalar/fetch/*");
		context.addServlet(new ServletHolder(new DefaultServlet()), "/*");
		server.start();
	}

	public static class BigDawgFetchServlet extends HttpServlet {
		private static final long serialVersionUID = 6537664694070363096L;
		private static final String wait = "wait";
		private static final String fail = "fail";
		private UserMap users;
		private WorkQueue wq;

		public void init() {
			wq = new WorkQueue(backend_port);
			wq.init();
			users = new UserMap();
		}

		protected synchronized void fetchFirstTile(String user, HttpServletRequest request,
				HttpServletResponse response) throws IOException {

			//TODO: fix this
			//String tile_id = request.getParameter("tile_id");
			//String query = request.getParameter("query");
			String hashed_query = DBInterface.hashed_query;

			String jobid = wq.queueNewJob(user, "0_0_0", hashed_query);
			response.getWriter().println("\""+jobid+"\"");
		}

		protected synchronized void fetchTile(String user, HttpServletRequest request,
				HttpServletResponse response) throws IOException {

			//TODO: fix this
			String tile_id = request.getParameter("tile_id");
			//String query = request.getParameter("query");
			String hashed_query = DBInterface.hashed_query;

			String jobid = wq.queueNewJob(user, tile_id, hashed_query);
			response.getWriter().println("\""+jobid+"\"");
		}

		protected synchronized void getJobResult(String user, HttpServletRequest request,
				HttpServletResponse response) throws IOException {
			String jobid = request.getParameter("jobid");
			byte[] data = wq.getJobResults(jobid);
			if(data == null) {
				if(wq.checkJobFail(jobid)) { // job failed
					response.getWriter().println("\""+fail+"\"");
				} else { // not ready yet
					response.getWriter().println("\""+wait+"\"");
				}
			} else { // results ready
				//response.getOutputStream().write(data,0,data.length);
				NiceTile t = NiceTilePacker.unpackNiceTile(data);
				String toSend = NiceTilePacker.makeJson(t);
				response.getWriter().println(toSend);
			}
		}

		protected String getNewUserId() {
			// generate a unique user id and send it back to the browser
			return UUID.randomUUID().toString();
		}

		protected void doGet(HttpServletRequest request,
				HttpServletResponse response) throws ServletException, IOException {

			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);

			String user = request.getParameter("user");

			String getUser = request.getParameter("guid");
			if(getUser != null) {
				user = getNewUserId();
				response.getWriter().println("\""+user+"\"");
			}
			users.put(user);

			String fft = request.getParameter("fft");
			if(fft != null) {
				fetchFirstTile(user,request,response);
			}

			String ft = request.getParameter("ft");
			if(ft != null) {
				fetchTile(user,request,response);
			}

			String getResult = request.getParameter("gr");
			if(getResult != null) {
				getJobResult(user,request,response);
			}

			String reset = request.getParameter("reset");
			if(reset != null) {
				response.getWriter().println();
			}

			users.prune();
		}

		public void destroy() {
			wq.shutdown();
		}
	}
}
