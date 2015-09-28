package frontend.BigDawg;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import utils.DBInterface;
import utils.UtilityFunctions;
import backend.util.TileKey;
import frontend.PreCompClient;
import frontend.BigDawg.TileMap;
import frontend.BigDawg.UserTilePairing;

public class WorkQueue extends PreCompClient {
	private Map<String,UserTilePairing> jobParameters;
	private TileMap tileMap;
	private Map<String,String> jobErrors;
	private ExecutorService executorService;
	private int backend_port;

	public WorkQueue(int backend_port) {
		this.jobParameters = new HashMap<String,UserTilePairing>();
		this.tileMap = new TileMap();
		this.jobErrors = new HashMap<String,String>();
		this.backend_port = backend_port;
	}

	public void init() {
		executorService = (ExecutorService) Executors.newFixedThreadPool(2);
	}
	
	public void shutdown() {
		executorService.shutdown();
	}
	
	public synchronized boolean checkJobFail(String jobid) {
		String fail = jobErrors.remove(jobid);
		return fail != null;
	}
	
	public synchronized boolean checkJobResults(String jobid) {
		return tileMap.peek(jobid);
	}
	
	public synchronized byte[] getJobResults(String jobid) {
		byte[] data = tileMap.pop(jobid);
		if(data != null) jobParameters.remove(jobid);
		return data;
	}
	
	public synchronized String queueNewJob(String user, String tile_id, String hashed_query) {
		String jobid = getNewJobId(user,tile_id);
		submitNewTask(jobid,user,tile_id,hashed_query);
		return jobid;
	}
	
	private synchronized String getNewJobId(String user, String tile_id) {
		UserTilePairing curr = new UserTilePairing();
		curr.user = user;
		curr.tile_id = tile_id;
		String jobid = UUID.randomUUID().toString();
		jobParameters.put(jobid, curr);
		return jobid;
	}
	
	private synchronized void submitNewTask(String jobid, String user, String tile_id, String hashed_query) {
		WorkQueueTask task = new WorkQueueTask();
		task.jobid = jobid;
		task.user = user;
		task.tile_id = tile_id;
		task.hashed_query = hashed_query;
		executorService.submit(new FutureTask<Object>(task,null));
	}
	
	private String buildUrlParamsFast(String user, String hashed_query, String dims, int zoom) {
		String params = "";
		//params += "hashed_query="+hashed_query;
		params += "threshold=" + DBInterface.threshold;
		params += "&zoom=" + zoom;
		params += "&tile_id=" + dims;
		params += "&user=" + user;
		return params;
	}
	
	private String buildUrlParams(String user, String hashed_query, TileKey tile_id) {
			String params = "";
			//params += "hashed_query="+hashed_query;
			params += "threshold=" + DBInterface.threshold;
			params += "&zoom=" + tile_id.zoom;
			String dims = "";
			if(tile_id.id.length > 0) {
				dims += tile_id.id[0];
			}
			for(int i = 1; i < tile_id.id.length; i++) {
				dims += "_"+tile_id.id[i];
			}
			params += "&tile_id=" + dims;
			params += "&user=" + user;
			return params;
	}
	
	private int[] getDims(String dims) {
		String[] tokens = dims.split("_");
		int[] result = new int[tokens.length];
		for(int i = 0; i < result.length; i++) {
			result[i] = Integer.parseInt(tokens[i]);
		}
		return result;
	}
	
	private synchronized boolean sendRequest(String jobid, String user, String tile_id, String hashed_query) throws Exception {
		int split = tile_id.indexOf("_");
		int zoom = Integer.parseInt(tile_id.substring(0,split));
		String dims = tile_id.substring(split+1);
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?fetch&" + buildUrlParamsFast(user,hashed_query, dims,zoom);
		TileKey key = new TileKey(getDims(dims),zoom);
		return sendRequestFromUrl(jobid,user,key,urlstring);
	}
	
	private synchronized boolean sendRequest(String jobid, String user, String tile_id, int zoom, String hashed_query) throws Exception {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?fetch&" + buildUrlParamsFast(user,hashed_query,tile_id,zoom);
		TileKey key = new TileKey(UtilityFunctions.parseTileIdInteger(tile_id),zoom);
		return sendRequestFromUrl(jobid,user,key,urlstring);
	}
	
	private synchronized boolean sendRequest(String jobid, String user, TileKey tile_id, String hashed_query) throws Exception {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?fetch&" + buildUrlParams(user,hashed_query, tile_id);
		return sendRequestFromUrl(jobid,user, tile_id,urlstring);
	}

	protected synchronized boolean sendRequestFromUrl(String jobid, String user, TileKey tile_id, String urlstring) throws Exception {
		URL geturl = null;
		HttpURLConnection connection = null;
		StringBuffer sbuffer = new StringBuffer();
		String result = null;
		long diff = 0;
		System.out.println("url:"+urlstring);
		geturl = new URL(urlstring);
		connection = (HttpURLConnection) geturl.openConnection();
		diff = System.currentTimeMillis();
		InputStream is = connection.getInputStream();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		
		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
		  buffer.write(data, 0, nRead);
		}

		buffer.flush();
		diff = System.currentTimeMillis() - diff;
		result = sbuffer.toString();
		//System.out.println("tile ("+tile_id+", "+zoom+") result length: " + result.length());
		if(result.equals("error")) {
			connection.disconnect();
			throw new Exception("serious error occurred on backend while retrieving tile");
		}
		byte[] rawBytes = buffer.toByteArray();
		//NiceTile tile = NiceTilePacker.unpackNiceTile(rawBytes); // unpack tile
		//if(tile.getSize() == 0) return false;
		tileMap.push(jobid,rawBytes);

		connection.disconnect();
		return true;
	}
	
	private class WorkQueueTask implements Runnable {
		public String jobid;
		public String user;
		public String tile_id;
		public String hashed_query;
		
		public synchronized void run() {
			try {
				sendRequest(jobid, user, tile_id, hashed_query);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				jobErrors.put(jobid, e.getMessage());
			}
		}
	}
}
