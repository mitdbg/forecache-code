package abstraction.mdstorage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import abstraction.mdstructures.DefinedTileView;
import abstraction.mdstructures.MultiDimTileKey;
import abstraction.tile.MultiDimColumnBasedNiceTile;

/**
 * @author leibatt
 * This class tracks the various caching levels on the backend (minus client-side cache)
 */
public class TileCacheManager {
	//protected TileBuffer mainMemoryBuffer;
	//protected TileBuffer diskBuffer;
	protected TileBuffer buffer;
	protected DefinedTileView dtv;
	
	protected int totalRequests = 0;
	protected int cacheHits = 0;
	protected List<String> hitsList;
	
	protected PreFetchTask prefetchTask;
	protected Future<?> prefetchFuture = null;
	protected ExecutorService threadPool = null;
	protected int poolsize = 1;
	
	public TileCacheManager(DefinedTileView dtv, TileBuffer buffer) {
		this.dtv = dtv;
		this.buffer = buffer;
		this.hitsList = new ArrayList<String>();
	}
	
	public synchronized void init() {
		if(threadPool == null) {
			this.threadPool = Executors.newFixedThreadPool(poolsize);
		}
	}
	
	public synchronized void shutdown() {
		if(threadPool != null) {
			stopCurrentPrefetchTask();
			this.threadPool.shutdown();
		}
	}
	
	public synchronized void reset(DefinedTileView dtv, int buffer_size) {
		clear();
		this.dtv = dtv;
		this.buffer.resize(buffer_size);
	}
	
	public synchronized void clear() {
		stopCurrentPrefetchTask();
		this.totalRequests = 0;
		this.cacheHits = 0;
		this.hitsList.clear();
		this.buffer.clear();
	}
	
	public synchronized boolean isReady() {
		return (this.prefetchFuture == null) || (this.prefetchFuture.isDone());
	}
	
	public synchronized void insertPredictions(List<MultiDimTileKey> predictions) {
		startNewPrefetchTask(predictions);
	}
	
	// back door used to quickly get tiles for metadata purposes
	public synchronized MultiDimColumnBasedNiceTile getTileForMetadata(MultiDimTileKey key) {
		return this.dtv.getTileForMetadata(key);
	}
	
	// answer a user request
	/**
	 * @param key
	 * @return
	 */
	public synchronized MultiDimColumnBasedNiceTile retrieveUserRequestedTile(MultiDimTileKey key) {
		// stop prefetching and look for this tile
		boolean found = updateAccuracy(key);
		// is it in the cache?
		if(found) {
			return this.buffer.get(key);
		}
		MultiDimColumnBasedNiceTile tile = new MultiDimColumnBasedNiceTile();
		tile.id = key;
		boolean success = this.dtv.nti.getTile(this.dtv.v, this.dtv.ts, tile);
		// insert the result into the cache
		if(success) {
			this.buffer.insert(tile);
			return tile;
		}
		return null;
	}
	
	public boolean updateAccuracy(MultiDimTileKey id) {
		stopCurrentPrefetchTask();
		// is it in LRU buffer or regular buffer?
		boolean found = this.buffer.peek(id);
		if(found) {
			this.cacheHits++;
			this.hitsList.add("hit");
		} else {
			this.hitsList.add("miss");
		}
		totalRequests++;
		return found;
	}
	
	public double getAccuracy() {
		return 1.0 * cacheHits / totalRequests;
	}

	public String getFullAccuracy() {
		if(hitsList.size() == 0) {
			return "[]";
		}
		String res = hitsList.get(0);
		for(int i = 1; i < hitsList.size(); i++) {
			res = res + ","+hitsList.get(i);
		}
		return res;
	}

	public String[] getFullAccuracyRaw() {
		return hitsList.toArray(new String[hitsList.size()]);
	}
	
	/*************** helper functions ****************/
	
	// answer a prediction request
	protected void insertPredictedTile(MultiDimTileKey key) {
		// is it in the cache?
		if(this.buffer.peek(key)) {
			this.buffer.get(key); // get but don't return
			return;
		}
		MultiDimColumnBasedNiceTile tile = new MultiDimColumnBasedNiceTile();
		tile.id = key;
		boolean success = this.dtv.nti.getTile(this.dtv.v, this.dtv.ts, tile);
		// insert the result into the cache
		if(success) this.buffer.insert(tile);
	}
	
	protected void stopCurrentPrefetchTask() {
		if((this.prefetchFuture != null) &&
				!this.prefetchFuture.isDone()) {
			this.prefetchTask.stop = true; // should allow us to stop cleanly
			try {
				this.prefetchFuture.get(); // wait for the job to stop
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	protected void startNewPrefetchTask(List<MultiDimTileKey> toPrefetch) {
		stopCurrentPrefetchTask(); // stop the existing prefetch task
		this.prefetchTask = new PreFetchTask();
		this.prefetchTask.toPrefetch = toPrefetch;
		this.prefetchFuture = this.threadPool.submit(this.prefetchTask);
	}
	
	/********************* Nested Classes ************************/
	// this Task is used to prefetch tiles one at a time.
	public class PreFetchTask implements Runnable {
		public volatile boolean stop = false;
		public volatile List<MultiDimTileKey> toPrefetch;
		
		public void run() {
			for(MultiDimTileKey key : toPrefetch) {
				if(stop) {
					System.out.println("received stop call. stopping early...");
					return;
				}
				insertPredictedTile(key);
			}
			System.out.println("stopping normally...");
		}
	}
}
