package abstraction.storage;

import java.util.List;

import abstraction.query.NewTileInterface;
import abstraction.util.ColumnBasedNiceTile;
import abstraction.util.NewTileKey;
import abstraction.util.TileStructure;
import abstraction.util.View;

/**
 * @author leibatt
 * This class tracks the various caching levels on the backend (minus client-side cache)
 */
public class TileCacheManager {
	protected TileBuffer mainMemoryBuffer;
	protected TileBuffer diskBuffer;
	protected TileStructure tileStructure;
	protected View view;
	protected NewTileInterface tileInterface;
	protected int totalRequests = 0;
	protected int cacheHits = 0;
	
	public TileCacheManager(View view, TileStructure tileStructure, NewTileInterface tileInterface,
			TileBuffer mmBuffer, TileBuffer dbDiskBuffer) {
		this.tileInterface = tileInterface;
		this.view = view;
		this.tileStructure = tileStructure;
		this.mainMemoryBuffer = mmBuffer;
		this.diskBuffer = dbDiskBuffer;
	}
	
	public synchronized void init() {
	}
	
	public synchronized void shutdown() {
	}
	
	public synchronized void clear() {
		this.totalRequests = 0;
		this.cacheHits = 0;
		this.mainMemoryBuffer.clear();
		this.diskBuffer.clear();
	}
	
	// answer a prediction request
	public synchronized void insertPredictedTile(NewTileKey key) {
		// is it in the cache?
		if(this.mainMemoryBuffer.peek(key)) {
			this.mainMemoryBuffer.get(key); // get but don't return
			return;
		}
		ColumnBasedNiceTile tile = new ColumnBasedNiceTile();
		tile.id = key;
		boolean success = this.tileInterface.getTile(this.view, this.tileStructure, tile);
		// insert the result into the cache
		if(success) this.mainMemoryBuffer.insert(tile);
	}
	
	// should not be synchronized, so it can be interrupted
	public void insertPredictions(List<NewTileKey> predictions) {
		for(NewTileKey key : predictions) {
			insertPredictedTile(key);
		}
	}
	
	// answer a user request
	/**
	 * @param key
	 * @return
	 */
	public synchronized ColumnBasedNiceTile retrieveRequestedTile(NewTileKey key) {
		this.totalRequests++;
		// is it in the cache?
		if(this.mainMemoryBuffer.peek(key)) {
			this.cacheHits++;
			return this.mainMemoryBuffer.get(key);
		}
		ColumnBasedNiceTile tile = new ColumnBasedNiceTile();
		tile.id = key;
		boolean success = this.tileInterface.getTile(this.view, this.tileStructure, tile);
		// insert the result into the cache
		if(success) {
			this.mainMemoryBuffer.insert(tile);
			return tile;
		}
		return null;
	}
	
	/*************** helper functions ****************/
}
