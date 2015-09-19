package abstraction.storage;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import abstraction.util.ColumnBasedNiceTile;
import abstraction.util.NewTileKey;

/**
 * @author leibatt
 * 
 * Class for managing the in-memory middleware tile cache.
 */
public class MainMemoryTileBuffer extends TileBuffer {
	protected Map<NewTileKey,ColumnBasedNiceTile> storage; // for storing tiles
	
	public MainMemoryTileBuffer(int storagemax, EvictionPolicy ep) {
		super(storagemax,ep);
		this.storage = new HashMap<NewTileKey,ColumnBasedNiceTile>();
	}

	@Override
	public synchronized boolean peek(NewTileKey key) {
		return this.storage.containsKey(key);
	}

	@Override
	public synchronized List<NewTileKey> getBufferState() {
		List<NewTileKey> x = new ArrayList<NewTileKey>();
		for(NewTileKey key : this.storage.keySet()) {
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
	protected synchronized boolean insert_helper(ColumnBasedNiceTile tile) {
		super.insert_helper(tile);
		this.storage.put(tile.id, tile);
		return true;
	}
	
	@Override
	protected synchronized boolean remove_helper(NewTileKey key) {
		this.storage.remove(key);
		return true;
	}

	@Override
	protected synchronized ColumnBasedNiceTile get_helper(NewTileKey key) {
		return this.storage.get(key);
	}
}
