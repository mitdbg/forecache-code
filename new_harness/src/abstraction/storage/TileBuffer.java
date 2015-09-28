package abstraction.storage;

import java.util.List;

import abstraction.util.ColumnBasedNiceTile;
import abstraction.util.NewTileKey;

public abstract class TileBuffer {
	protected EvictionPolicy ep;
	protected int storage_max;
	protected static final int DEFAULTMAX = 1; // default buffer size
	
	public TileBuffer(int storage_max, EvictionPolicy ep) {
		this.ep = ep;
		this.storage_max = storage_max;
	}
	
	public abstract boolean peek(NewTileKey key);
	public abstract List<NewTileKey> getBufferState();
	public abstract int size();

	public synchronized ColumnBasedNiceTile get(NewTileKey key) {
		ColumnBasedNiceTile tile = get_helper(key);
		// if get was successful, update Eviction Policy
		if(tile != null) {
			this.ep.evaluateGet(key);
		}
		return tile;
	}

	public synchronized void insert(ColumnBasedNiceTile tile) {
		boolean inserted = insert_helper(tile);
		// if get was successful, update Eviction Policy
		if(inserted) this.ep.evaluateInsert(tile.id);
	}

	public synchronized void remove(NewTileKey key) {
		boolean removed = remove_helper(key);
		// if get was successful, update Eviction Policy
		if(removed) this.ep.evaluateRemove(key);
	}
	
	public synchronized void remove() {
		NewTileKey key = this.ep.getNextToRemove();
		remove(key);
	}

	public synchronized void clear() {
		for(NewTileKey key : getBufferState()) {
			remove(key);
		}
	}
	
	/************ Helper functions ************/
	
	protected abstract boolean remove_helper(NewTileKey key);
	protected abstract ColumnBasedNiceTile get_helper(NewTileKey key);
	
	// TODO: override this function
	// call super and add functionality
	protected synchronized boolean insert_helper(ColumnBasedNiceTile tile) {
		// make room to do the insert
		if(size() == this.storage_max) {
			remove();
		}
		return false;
	}
}
