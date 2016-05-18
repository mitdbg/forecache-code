package abstraction.mdstorage;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import abstraction.mdstructures.MultiDimTileKey;
import abstraction.tile.MultiDimColumnBasedNiceTile;

/**
 * @author leibatt
 * 
 * Class for managing the in-memory middleware tile cache.
 */
public class MainMemoryTileBuffer extends TileBuffer {
	protected Map<MultiDimTileKey,MultiDimColumnBasedNiceTile> storage; // for storing tiles
	
	public MainMemoryTileBuffer(int storagemax, EvictionPolicy ep) {
		super(storagemax,ep);
		this.storage = new HashMap<MultiDimTileKey,MultiDimColumnBasedNiceTile>();
	}

	@Override
	public synchronized boolean peek(MultiDimTileKey key) {
		return this.storage.containsKey(key);
	}

	@Override
	public synchronized List<MultiDimTileKey> getBufferState() {
		List<MultiDimTileKey> x = new ArrayList<MultiDimTileKey>();
		for(MultiDimTileKey key : this.storage.keySet()) {
			x.add(key);
		}
		return x;
	}

	@Override
	public synchronized int size() {
		return this.storage.size();
	}

	/*************** Helper Functions ****************/

	@Override
	protected synchronized boolean insert_helper(MultiDimColumnBasedNiceTile tile) {
		super.insert_helper(tile);
		this.storage.put(tile.id, tile);
		return true;
	}
	
	@Override
	protected synchronized boolean remove_helper(MultiDimTileKey key) {
		this.storage.remove(key);
		return true;
	}

	@Override
	protected synchronized MultiDimColumnBasedNiceTile get_helper(MultiDimTileKey key) {
		return this.storage.get(key);
	}
}
