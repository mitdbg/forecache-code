package abstraction.mdstructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import abstraction.tile.MultiDimColumnBasedNiceTile;


/**
 * @author leibatt
 * Class for keeping track of the client request history
 */
public class TileHistoryQueue {
	private ArrayList<TileRecord> history;
	private ArrayList<TileRecord> trueHistory; // used to track ROI's
	protected List<MultiDimTileKey> lastRoi;
	protected List<MultiDimColumnBasedNiceTile> lastRoiTiles;
	protected int lastZoomOut = -1;
	protected boolean newRoi = false;
	private int maxhist;
	
	public TileHistoryQueue(int maxhist) {
		history = new ArrayList<TileRecord>();
		trueHistory = new ArrayList<TileRecord>();
		lastRoi = new ArrayList<MultiDimTileKey>();
		this.maxhist = maxhist;
		this.lastRoiTiles = new ArrayList<MultiDimColumnBasedNiceTile>();
	}
	
	public synchronized void clear() {
		history.clear();
		trueHistory.clear();
		lastRoi.clear();
		lastRoiTiles.clear();
	}
	
	// adds record to history for given tile
	// copies entire tile
	// maintains history of length maxhist
	public synchronized void addRecord(MultiDimColumnBasedNiceTile Next) {
		history.add(new TileRecord(Next));
		trueHistory.add(new TileRecord(Next));
		updateROI();
		if(history.size() > maxhist) {
			history.remove(0);
		}
	}
	
	// returns a clone of the record at given index
	// will throw exception if index is outside bounds
	public synchronized final MultiDimColumnBasedNiceTile getRecordTile(int index) {
		return history.get(index).MyTile;
	}
	
	// returns a clone of the tile id of the record at given index
	// will throw exception if index is outside bounds
	public synchronized final MultiDimTileKey getRecordNewTileKey(int index) {
		return history.get(index).MyTile.id;
	}
	
	// returns the timestamp of the record at the given index
	public synchronized final long getRecordTimestamp(int index) {
		return history.get(index).timestamp;
	}
	
	// returns the length of the history
	public synchronized int getHistoryLength() {
		return history.size();
	}
	
	// returns history as user requests for directional models
	public synchronized List<UserRequest> getHistoryTrace() {
		List<UserRequest> myresult = new ArrayList<UserRequest>();
		for(int i = 0; i < history.size(); i++) {
			TileRecord tr = history.get(i);
			MultiDimTileKey tk = tr.MyTile.id;
			UserRequest temp = new UserRequest(tk.buildTileString(),tk.zoom);
			myresult.add(temp);
		}
		return myresult;
	}
	
	public synchronized MultiDimTileKey getLast() {
		if(history.size() == 0) return null;
		return history.get(history.size() - 1).MyTile.id;
	}
	
	// returns last k elements in history as user requests for directional models
	public synchronized List<MultiDimTileKey> getHistoryTrace(int length) {
		List<MultiDimTileKey> myresult = new ArrayList<MultiDimTileKey>();
		int start = history.size() - length;
		if(start < 0) {
			start = 0;
		}
		for(int i = start; i < history.size(); i++) {
			TileRecord tr = history.get(i);
			myresult.add(tr.MyTile.id);
		}
		return myresult;
	}
	
	// get the keys corresponding to the last ROI
	public synchronized List<MultiDimTileKey> getLastRoi() {
		if(lastRoi.size() > 0) {
			return lastRoi;
		}
		
		// just return the last request, if there is no ROI yet
		List<MultiDimTileKey> makeshiftRoi = new ArrayList<MultiDimTileKey>();
		if(history.size() == 0) return makeshiftRoi;
		makeshiftRoi.add(history.get(history.size()-1).MyTile.id);
		return makeshiftRoi;
	}
	
	// get the tiles corresponding to the last ROI
	public synchronized List<MultiDimColumnBasedNiceTile> getLastRoiTiles() {
		if(lastRoiTiles.size() > 0) {
			return lastRoiTiles;
		}
		
		// just return the last request, if there is no ROI yet
		List<MultiDimColumnBasedNiceTile> makeshiftRoi = new ArrayList<MultiDimColumnBasedNiceTile>();
		if(history.size() == 0) return makeshiftRoi;
		MultiDimColumnBasedNiceTile last = history.get(history.size()-1).MyTile;
		makeshiftRoi.add(last);
		return makeshiftRoi;
	}
	
	public synchronized boolean newRoi() {
		return newRoi;
	}
	
	// find the user's last region of interest!
	protected synchronized void updateROI() {
		int lastZoomOut = -1;
		int lastZoomIn = -1;
		int i = trueHistory.size() - 2;
		for(;i > this.lastZoomOut; i--) {
			MultiDimTileKey lastKey = trueHistory.get(i+1).MyTile.id;
			MultiDimTileKey nextLastKey = trueHistory.get(i).MyTile.id;
			//if(lastKey.zoom < nextLastKey.zoom) { // found zoom out
			if(lastKey.compareZoom(nextLastKey) < 0) { // found zoom out
					lastZoomOut = i;
					break;
			}
		}
		for(;i > this.lastZoomOut; i--) {
			MultiDimTileKey lastKey = trueHistory.get(i+1).MyTile.id;
			MultiDimTileKey nextLastKey = trueHistory.get(i).MyTile.id;
			//if(lastKey.zoom > nextLastKey.zoom) { // found zoom in, new ROI!
			if(lastKey.compareZoom(nextLastKey) > 0) { // found zoom in, new ROI!
				lastZoomIn = i;
				break;
			//} else if (lastKey.zoom < nextLastKey.zoom) { // found another zoom out, abort!
			} else if (lastKey.compareZoom(nextLastKey) < 0) { // found another zoom out, abort!
				lastZoomOut = -1;
				break;
			}
		}
		
		if(lastZoomOut >= 0 && lastZoomIn >= 0) {
			this.lastZoomOut = lastZoomOut;
			lastRoi.clear();
			lastRoiTiles.clear();
			for(i = lastZoomIn; i <= lastZoomOut; i++) {
				MultiDimColumnBasedNiceTile curr = history.get(i).MyTile;
				lastRoi.add(curr.id);
				lastRoiTiles.add(curr);
			}
			newRoi = true;
		} else {
			newRoi = false;
		}
	}
	
	@Override
	public synchronized String toString() {
		StringBuilder myresult = new StringBuilder();
		myresult.append("{").append("\n");
		for(int i = 0; i < history.size(); i++) {
			TileRecord record = history.get(i);
			myresult.append("\t");
			myresult.append(record);
			myresult.append("\n");
		}
		myresult.append("}");
		return myresult.toString();
	}

	private class TileRecord {
		public long timestamp;
		public MultiDimColumnBasedNiceTile MyTile;
		
		public TileRecord(MultiDimColumnBasedNiceTile Next) {
			this.timestamp = System.currentTimeMillis() / 1000;
			this.MyTile = Next;
		}
		
		@Override
		public String toString() {
			StringBuilder myresult = new StringBuilder();
			myresult.append("{");
			myresult.append(this.timestamp);
			myresult.append(":");
			myresult.append(this.MyTile);
			myresult.append("}");
			return myresult.toString();
		}
	}
}
