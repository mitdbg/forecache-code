package backend.util;

import java.util.Comparator;

/**
 * @author leibatt
 * class used to establish LRU eviction order using timestamps
 */
public class TimePair {
	private long timestamp;
	private TileKey id;
	
	public TimePair(TileKey id) {
		this.id = id;
		this.timestamp = System.currentTimeMillis();
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
	
	// comparator class for ordering lruQueue using timestamps
	public static class TPSort implements Comparator<TimePair> {
		@Override
		public int compare(TimePair t1, TimePair t2) {
			return (int) (t1.getTimestamp() - t2.getTimestamp());
		}
	}
}
