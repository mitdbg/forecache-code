package abstraction.mdstorage;

import java.util.List;


import abstraction.mdstructures.MultiDimTileKey;
import abstraction.tile.MultiDimColumnBasedNiceTile;

public abstract class TileBuffer {
	protected EvictionPolicy ep;
	protected int storage_max;
	protected static final int DEFAULTMAX = 1; // default buffer size
	
	public TileBuffer(int storage_max, EvictionPolicy ep) {
		this.ep = ep;
		this.storage_max = storage_max;
	}
	
	public abstract boolean peek(MultiDimTileKey key);
	public abstract List<MultiDimTileKey> getBufferState();
	public abstract int size();
	
	public synchronized void resize(int storage_max) {
		this.storage_max = storage_max;
	}
	
	public synchronized void resizeAndClear(int storage_max) {
		resize(storage_max);
		clear();
	}

	// do not evaluate the eviction policy here
	public synchronized MultiDimColumnBasedNiceTile ignoredGet(MultiDimTileKey key) {
		return get_helper(key);
	}
	
	public synchronized MultiDimColumnBasedNiceTile get(MultiDimTileKey key) {
		MultiDimColumnBasedNiceTile tile = get_helper(key);
		// if get was successful, update Eviction Policy
		if(tile != null) {
			this.ep.evaluateGet(key);
		}
		return tile;
	}

	public synchronized void insert(MultiDimColumnBasedNiceTile tile) {
		boolean inserted = insert_helper(tile);
		// if get was successful, update Eviction Policy
		if(inserted) this.ep.evaluateInsert(tile.id);
	}

	public synchronized void remove(MultiDimTileKey key) {
		boolean removed = remove_helper(key);
		// if get was successful, update Eviction Policy
		if(removed) this.ep.evaluateRemove(key);
	}
	
	public synchronized void remove() {
		MultiDimTileKey key = this.ep.getNextToRemove();
		remove(key);
	}

	public synchronized void clear() {
		for(MultiDimTileKey key : getBufferState()) {
			remove(key);
		}
	}
	
	/************ Helper functions ************/
	
	protected abstract boolean remove_helper(MultiDimTileKey key);
	protected abstract MultiDimColumnBasedNiceTile get_helper(MultiDimTileKey key);
	
	// TODO: override this function
	// call super and add functionality
	protected synchronized boolean insert_helper(MultiDimColumnBasedNiceTile tile) {
		// make room to do the insert
		if(size() == this.storage_max) {
			remove();
		}
		return false;
	}
}
