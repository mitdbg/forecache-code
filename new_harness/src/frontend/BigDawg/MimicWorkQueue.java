package frontend.BigDawg;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import abstraction.util.NewTileKey;

public class MimicWorkQueue {
	private String backend_host = "localhost";
	private int backend_port = 8080;
	private String backend_root = "gettile";
	
	private Map<String,MimicUserTilePairing> jobParameters;
	private MimicTileMap tileMap;
	private Map<String,String> jobErrors;
	private ExecutorService executorService;

	public MimicWorkQueue(int backend_port) {
		this.jobParameters = new HashMap<String,MimicUserTilePairing>();
		this.tileMap = new MimicTileMap();
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
	
	public synchronized String getJobResults(String jobid) {
		String data = tileMap.pop(jobid);
		if(data != null) jobParameters.remove(jobid);
		return data;
	}
	
	public synchronized String queueNewJob(String user, int k, int aggWindow, String recordName, int tile_id) {
		String jobid = getNewJobId(user,k,aggWindow,recordName,tile_id);
		submitNewTask(jobid,user,k,aggWindow,recordName,tile_id);
		return jobid;
	}
	
	private synchronized String getNewJobId(String user,int k, int aggWindow, String recordName, int tile_id) {
		MimicUserTilePairing curr = new MimicUserTilePairing();
		curr.user = user;
		curr.tile_id = tile_id;
		curr.k = k;
		curr.aggWindow = aggWindow;
		curr.recordName = recordName;
		String jobid = UUID.randomUUID().toString();
		jobParameters.put(jobid, curr);
		return jobid;
	}
	
	private synchronized void submitNewTask(String jobid, String user, int k, int aggWindow,
			String recordName, int tile_id) {
		MimicWorkQueueTask task = new MimicWorkQueueTask();
		task.jobid = jobid;
		task.user = user;
		task.tile_id = tile_id;
		task.k = k;
		task.aggWindow = aggWindow;
		task.recordName = recordName;
		executorService.submit(new FutureTask<Object>(task,null));
	}
	
	private String buildUrlParams(String user, int k, int aggWindow, String recordName,
			int tile_id) {
		String params = "";
		params += "k=" + k;
		params += "&aggWindow=" + aggWindow;
		params += "&recordName=" + recordName;
		params += "&tile_id=" + tile_id;
		params += "&user=" + user;
		return params;
	}
	
	private synchronized boolean sendRequest(String jobid, String user, int k,
			int aggWindow, String recordName, int tile_id) throws Exception {
		String urlstring = "http://"+backend_host+":"+backend_port+"/"+backend_root + "/"
				+ "?fetch&" + buildUrlParams(user,k,aggWindow,recordName,tile_id);
		NewTileKey key = new NewTileKey(new int[]{tile_id},aggWindow);
		return sendRequestFromUrl(jobid,user,key,urlstring);
	}

	protected synchronized boolean sendRequestFromUrl(String jobid, String user, NewTileKey tile_id, String urlstring) throws Exception {
		URL geturl = null;
		HttpURLConnection connection = null;
		StringBuffer sbuffer = new StringBuffer();
		String result = null;
		long diff = 0;
		System.out.println("url:"+urlstring);
		geturl = new URL(urlstring);
		connection = (HttpURLConnection) geturl.openConnection();
		diff = System.currentTimeMillis();
		String line;
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		while((line = reader.readLine()) != null) {
			sbuffer.append(line);
		}
		diff = System.currentTimeMillis() - diff;
		result = sbuffer.toString();

		reader.close();
		//System.out.println("tile ("+tile_id+", "+zoom+") result length: " + result.length());
		if(result.equals("error")) {
			connection.disconnect();
			throw new Exception("serious error occurred on backend while retrieving tile");
		}
		//NiceTile tile = NiceTilePacker.unpackNiceTile(rawBytes); // unpack tile
		//if(tile.getSize() == 0) return false;
		tileMap.push(jobid,result);

		connection.disconnect();
		return true;
	}
	
	private class MimicWorkQueueTask implements Runnable {
		public String jobid;
		public String user;
		public int tile_id;
		public int k;
		public int aggWindow;
		public String recordName;
		
		public synchronized void run() {
			try {
				sendRequest(jobid, user, k,aggWindow,recordName, tile_id);
				//want to make a synchronized call to the appropriate tile interface here,
				// instead of a (slow) second http request
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				jobErrors.put(jobid, e.getMessage());
			}
		}
	}
}
