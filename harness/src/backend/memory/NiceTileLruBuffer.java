package backend.memory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import backend.util.NiceTile;
import backend.util.TileKey;

// used to keep track of user's last x moves, where x=bufferSize
public class NiceTileLruBuffer {
	protected Map<TileKey,NiceTile> qMap;
	protected Queue<TileKey> q;
	public static int defaultSize = 4;
	protected int bufferSize;
	
	public NiceTileLruBuffer(int bufferSize) {
		this.bufferSize = bufferSize;
		q = new LinkedList<TileKey>();
		qMap = new HashMap<TileKey,NiceTile>();
	}
	
	// check if in buffer
	public boolean peek(TileKey id) {
		return qMap.containsKey(id);
	}
	
	// get tile but don't remove from queue
	public NiceTile getTile(TileKey id) {
		return qMap.get(id);
	}
	
	// remove first item from queue
	protected NiceTile dequeue() {
		TileKey toRemove = q.remove();
		return qMap.remove(toRemove);
	}

	// add tile to queue
	public void insertTile(NiceTile tile) {
		q.add(tile.id);
		if(!peek(tile.id)) qMap.put(tile.id, tile);
		if(q.size() > bufferSize) {
			dequeue();
		}
	}

	public int tileCount() {
		return q.size();
	}

	public void clear() {
		q.clear();
		qMap.clear();
	}

}
