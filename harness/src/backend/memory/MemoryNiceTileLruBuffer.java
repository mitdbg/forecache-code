package backend.memory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.TileKey;

// used to keep track of user's last x moves, where x=bufferSize
public class MemoryNiceTileLruBuffer implements NiceTileBuffer {
	protected Map<TileKey,NiceTile> qMap;
	protected Queue<TileKey> q;
	public static final int DEFAULTMAX = 4;
	protected int storagemax;
	
	public MemoryNiceTileLruBuffer(int storagemax) {
		this.storagemax = storagemax;
		q = new LinkedList<TileKey>();
		qMap = new HashMap<TileKey,NiceTile>();
	}
	
	public synchronized void setStorageMax(int storagemax) {
		this.storagemax = storagemax;
		clear();
	}
	
	// check if in buffer
	public synchronized boolean peek(TileKey id) {
		return qMap.containsKey(id);
	}
	
	public synchronized Set<TileKey> getAllTileKeys() {
		return qMap.keySet();
	}
	
	// get tile but don't remove from queue
	public synchronized NiceTile getTile(TileKey id) {
		if(peek(id)) {
			q.remove(id);
			q.add(id);
			return qMap.get(id);
		}
		return null;
	}
	
	// remove first item from queue
	protected synchronized void dequeue() {
		if(tileCount() == 0) return;
		TileKey toRemove = q.remove();
		qMap.remove(toRemove);
	}

	// add tile to queue
	public synchronized void insertTile(NiceTile tile) {
		if(storagemax == 0) return;
		while(tileCount() >= storagemax) {
			dequeue();
		}
		q.add(tile.id);
		qMap.put(tile.id, tile);
	}
	
	public synchronized void removeTile(TileKey id) {
		q.remove(id);
		qMap.remove(id);
	}
	
	public synchronized void touchTile(TileKey id) {
		q.remove(id);
		q.add(id);
	}

	public synchronized int tileCount() {
		return qMap.size();
	}

	public synchronized void clear() {
		q.clear();
		qMap.clear();
	}

}
