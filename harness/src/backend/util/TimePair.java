package backend.util;

import java.util.Comparator;

/**
 * @author leibatt
 * class used to establish LRU eviction order using timestamps
 */
public class TimePair {
	private long timestamp;
	private TileKey id;
	private int tilesize;
	
	public TimePair(TileKey id) {
		this.id = id;
		this.timestamp = System.currentTimeMillis();
		this.tilesize = 0;
	}
	
	public TimePair(TileKey id, int tilesize) {
		this.id = id;
		this.timestamp = System.currentTimeMillis();
		this.tilesize = tilesize;
	}
	
	// for inserting pairs at a specific time (e.g. initialization of cache)
	public TimePair(TileKey id, long timestamp, int tilesize) {
		this.id = id;
		this.timestamp = timestamp;
		this.tilesize = tilesize;
	}
	
	public final long getTimestamp() {
		return this.timestamp;
	}
	
	public void updateTimestamp() {
		this.timestamp = System.currentTimeMillis();
	}
	
	public TileKey getTileKey() {
		return this.id;
	}
	
	public int getTileSize() {
		return this.tilesize;
	}
	
	// comparator class for ordering lruQueue using timestamps
	public static class TPSort implements Comparator<TimePair> {
		@Override
		public int compare(TimePair t1, TimePair t2) {
			return (int) (t1.getTimestamp() - t2.getTimestamp());
		}
	}
}
