package abstraction.storage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.tile.ColumnBasedNiceTile;

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
	PreFetchTask prefetchTask;
	Future<?> prefetchFuture = null;
	ExecutorService threadPool = null;
	int poolsize = 1;
	
	public TileCacheManager(DefinedTileView dtv, TileBuffer buffer) {
		this.dtv = dtv;
		this.buffer = buffer;
	}
	
	public synchronized void init() {
		this.threadPool = Executors.newFixedThreadPool(poolsize);
	}
	
	public synchronized void shutdown() {
		stopCurrentPrefetchTask();
		this.threadPool.shutdown();
	}
	
	public synchronized void clear() {
		stopCurrentPrefetchTask();
		this.totalRequests = 0;
		this.cacheHits = 0;
		this.buffer.clear();
	}
	
	public synchronized boolean isReady() {
		return (this.prefetchFuture == null) || (this.prefetchFuture.isDone());
	}
	
	// should not be synchronized, so it can be interrupted
	public synchronized void insertPredictions(List<NewTileKey> predictions) {
		startNewPrefetchTask(predictions);
	}
	
	// answer a user request
	/**
	 * @param key
	 * @return
	 */
	public synchronized ColumnBasedNiceTile retrieveRequestedTile(NewTileKey key) {
		stopCurrentPrefetchTask();
		this.totalRequests++;
		// is it in the cache?
		if(this.buffer.peek(key)) {
			this.cacheHits++;
			return this.buffer.get(key);
		}
		ColumnBasedNiceTile tile = new ColumnBasedNiceTile();
		tile.id = key;
		boolean success = this.dtv.nti.getTile(this.dtv.v, this.dtv.ts, tile);
		// insert the result into the cache
		if(success) {
			this.buffer.insert(tile);
			return tile;
		}
		return null;
	}
	
	/*************** helper functions ****************/
	
	// answer a prediction request
	protected void insertPredictedTile(NewTileKey key) {
		// is it in the cache?
		if(this.buffer.peek(key)) {
			this.buffer.get(key); // get but don't return
			return;
		}
		ColumnBasedNiceTile tile = new ColumnBasedNiceTile();
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
	
	protected void startNewPrefetchTask(List<NewTileKey> toPrefetch) {
		stopCurrentPrefetchTask(); // stop the existing prefetch task
		this.prefetchTask = new PreFetchTask();
		this.prefetchTask.toPrefetch = toPrefetch;
		this.prefetchFuture = this.threadPool.submit(this.prefetchTask);
	}
	
	/********************* Nested Classes ************************/
	/*this Task is used to prefetch tiles one at a time.*/
	public class PreFetchTask implements Runnable {
		public volatile boolean stop = false;
		public volatile List<NewTileKey> toPrefetch;
		
		public void run() {
			for(NewTileKey key : toPrefetch) {
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
