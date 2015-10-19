package abstraction.storage;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import abstraction.query.NewTileInterface;
import abstraction.structures.NewTileKey;
import abstraction.structures.ProgressMap;
import abstraction.structures.TileStructure;
import abstraction.structures.View;
import abstraction.tile.ColumnBasedNiceTile;

public class OldTileCacheManager {
	private Queue<NewTileKey> tileRequestQueue;
	private Queue<NewTileKey> tilePredictionQueue;
	private Object sharedQueueObject;
	private ProgressMap<String,Boolean> zoomLevelProgressMap;
	private ProgressMap<NewTileKey,ColumnBasedNiceTile> tileMap;
	private ProgressMap<NewTileKey,String> tileErrorMap;
	private ProgressMap<String,NewTileKey> jobsToTiles;
	private NewTileInterface tileInterface;
	private View view;
	private TileStructure tileStructure;
	private ExecutorService executorService;
	private TileCacheManageQueuesTask mainTask;
	
	public OldTileCacheManager(View view, TileStructure tileStructure, NewTileInterface tileInterface) {
		this.tileRequestQueue = new ConcurrentLinkedQueue<NewTileKey>();
		this.tilePredictionQueue = new ConcurrentLinkedQueue<NewTileKey>();
		this.sharedQueueObject = new Object();
		this.zoomLevelProgressMap = new ProgressMap<String,Boolean>();
		this.tileMap = new ProgressMap<NewTileKey,ColumnBasedNiceTile>();
		this.tileErrorMap = new ProgressMap<NewTileKey,String>();
		this.jobsToTiles = new ProgressMap<String,NewTileKey>();
		
		this.tileInterface = tileInterface;
		this.view = view;
		this.tileStructure = tileStructure;
	}
	
	public synchronized void init() {
		executorService = (ExecutorService) Executors.newFixedThreadPool(3);
		this.mainTask = new TileCacheManageQueuesTask();
		executorService.submit(this.mainTask);
	}
	
	public synchronized void shutdown() {
		// tell the main task to shutdown
		this.mainTask.shutdown = true;
		synchronized(this.sharedQueueObject) {
			sharedQueueObject.notify();
		}
		
		//shutdown the executor service
		executorService.shutdown();
	}
	
	// tell the cache manager to get the requested tile
	public synchronized String submitTileRequest(NewTileKey tileid) {
		String jobid = getNewJobId();
		this.tileRequestQueue.add(tileid);
		this.jobsToTiles.push(jobid, tileid);
		synchronized(this.sharedQueueObject) {
			this.sharedQueueObject.notify();
		}
		return jobid;
	}
	
	// tell the cache manager to get the predicted tile
	public synchronized String submitTilePrediction(NewTileKey tileid) {
		String jobid = getNewJobId();
		this.tilePredictionQueue.add(tileid);
		this.jobsToTiles.push(jobid, tileid);
		synchronized(this.sharedQueueObject) {
			this.sharedQueueObject.notify();
		}
		return jobid;
	}
	
	public synchronized boolean checkJobDone(String jobid) {
		boolean returnval = false;
		NewTileKey tileid = this.jobsToTiles.pop(jobid);
		if(tileid != null) {
			returnval = this.tileMap.peek(tileid) || this.tileErrorMap.peek(tileid);
		}
		this.jobsToTiles.push(jobid,tileid);
		return returnval;
	}
	
	public synchronized ColumnBasedNiceTile getJobResult(String jobid) {
		NewTileKey tileid = this.jobsToTiles.pop(jobid);
		if(this.tileMap.peek(tileid)) return this.tileMap.pop(tileid);
		this.jobsToTiles.push(jobid, tileid);
		return null;
	}
	
	public synchronized String getJobError(String jobid) {
		NewTileKey tileid = this.jobsToTiles.pop(jobid);
		if(this.tileErrorMap.peek(tileid)) return this.tileErrorMap.pop(tileid);
		this.jobsToTiles.push(jobid, tileid);
		return null;
	}
	
	/*************** helper functions ****************/
	
	protected String getNewJobId() {
		String jobid = UUID.randomUUID().toString();
		return jobid;
	}
	
	private class TileCacheManageQueuesTask implements Runnable {
		public boolean shutdown = false;
		public synchronized void run() {
			while(!shutdown) {
				while(!tileRequestQueue.isEmpty() && !tilePredictionQueue.isEmpty()) {
					if(shutdown) break;
					executorService.submit(new TileCacheProcessQueueTask());
				}
				while(tileRequestQueue.isEmpty() && tilePredictionQueue.isEmpty()) {
					if(shutdown) break;
					synchronized(sharedQueueObject) {
						try {
							sharedQueueObject.wait();
						} catch (InterruptedException e) {}
					}
				}
			}
		}
	}
	
	private class TileCacheProcessQueueTask implements Runnable {
		public NewTileKey tileid;
		boolean prediction;

		public synchronized void run() {
			// is there anything to do?
			this.tileid = tileRequestQueue.remove();
			if(this.tileid != null) { // new user request was submitted
				this.prediction = false;
				
				// assume all queued predictions are now invalid
				tilePredictionQueue.clear();
			}
			if(this.tileid == null) {
				this.tileid = tilePredictionQueue.remove();
				if (this.tileid != null) { // new prediction was submitted
					this.prediction = true;
				} else {
					// nothing to do!
					return;
				}
			}
			
			
			String zoomLevelName = tileInterface.getZoomLevelName(view, tileStructure, tileid.zoom);

			//zoom level is being built
			if(zoomLevelProgressMap.peek(zoomLevelName)) {
				if(prediction) {
					tilePredictionQueue.add(tileid);
				} else {
					tileRequestQueue.add(tileid);
				}
				return; // re-queue tasks and abort
				// zoom level is missing
			} else if (!tileInterface.checkZoomLevel(view, tileStructure, tileid.zoom)) {
				boolean go = false;
				// check to see if this still holds true
				synchronized(zoomLevelProgressMap) {
					if(!zoomLevelProgressMap.peek(zoomLevelName)) {
						zoomLevelProgressMap.push(zoomLevelName,true);
						go = true;
					}
				}
				if(go) {
					// build the zoom level
					tileInterface.buildZoomLevel(view, tileStructure, tileid.zoom);
					zoomLevelProgressMap.pop(zoomLevelName);
				}
			}
			// get the tile
			ColumnBasedNiceTile tile = new ColumnBasedNiceTile();
			tile.id = this.tileid;
			try {
				boolean result = tileInterface.retrieveStoredTile(view,tileStructure, tile,tile.id);
				if(result) {
					tileMap.push(this.tileid, tile);
				} else {
					tileErrorMap.push(this.tileid, "error occured while retrieving tile");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				tileErrorMap.push(this.tileid, e.getMessage());
			}
		}
	}
}
