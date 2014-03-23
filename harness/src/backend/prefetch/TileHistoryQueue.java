package backend.prefetch;

import java.util.ArrayList;

import backend.util.Tile;
import backend.util.TileKey;

/**
 * @author leibatt
 * Class for keeping track of the client request history
 */
public class TileHistoryQueue {
	private ArrayList<TileRecord> history;
	private int maxhist;
	
	public TileHistoryQueue(int maxhist) {
		history = new ArrayList<TileRecord>();
		this.maxhist = maxhist;
	}
	
	// adds record to history for given tile
	// copies entire tile
	// maintains history of length maxhist
	public synchronized void addRecord(Tile Next) {
		history.add(new TileRecord(Next));
		if(history.size() > maxhist) {
			history.remove(0);
		}
	}
	
	// returns a clone of the record at given index
	// will throw exception if index is outside bounds
	public synchronized final Tile getRecordTile(int index) {
		return history.get(index).MyTile;
	}
	
	// returns a clone of the tile id of the record at given index
	// will throw exception if index is outside bounds
	public synchronized final TileKey getRecordTileKey(int index) {
		return history.get(index).MyTile.getTileKey();
	}
	
	// returns the timestamp of the record at the given index
	public synchronized final long getRecordTimestamp(int index) {
		return history.get(index).timestamp;
	}
	
	// returns the length of the history
	public synchronized int getHistoryLength() {
		return history.size();
	}

	private class TileRecord {
		public long timestamp;
		public Tile MyTile;
		
		public TileRecord(Tile Next) {
			this.timestamp = System.currentTimeMillis() / 1000;
			this.MyTile = Next;
		}
	}
}
