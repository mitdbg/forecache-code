package backend.disk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import utils.DBInterface;

import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.TileKey;
import backend.util.TimePair;

/**
 * @author leibatt
 * This tile buffer does not actually delete any cooked tiles. It simply removes the label from the
 * cache.
 */
public class PreCompNiceTileBuffer implements NiceTileBuffer {
	protected Map<TileKey,TimePair> timeMap; // for finding things in the queue
	protected PriorityQueue<TimePair> lruQueue; // for identifying lru tiles in storage
	protected Map<TileKey,Boolean> isBuilt;
	protected int storagemax;
	protected int size;
	protected final int DEFAULTMAX = 1; // default buffer size
	protected final int initqueuesize = 50;
	protected String tileNamePrefix = DBInterface.arrayname+"_"; // default prefix
	protected String arrayname = DBInterface.arrayname;
	protected ScidbTileInterface sti;
	
	
	public PreCompNiceTileBuffer(ScidbTileInterface sti) throws Exception {
		this.sti = sti;
		this.isBuilt = new HashMap<TileKey,Boolean>();
		
		this.lruQueue = new PriorityQueue<TimePair>(this.initqueuesize,new TimePair.TPSort());
		this.timeMap = new HashMap<TileKey,TimePair>();
		this.size = 0;
		this.storagemax = this.DEFAULTMAX;
		findExistingTiles(); // check cache root for existing tiles
	}
	
	public PreCompNiceTileBuffer(ScidbTileInterface sti, int storagemax) throws Exception {
		this(sti);
		this.storagemax = storagemax;
	}
	
	public PreCompNiceTileBuffer(String tileNamePrefix, String arrayname, ScidbTileInterface sti) throws Exception {
		this(sti);
		this.tileNamePrefix = tileNamePrefix;
		this.arrayname = arrayname;
	}
	
	public PreCompNiceTileBuffer(String tileNamePrefix, String arrayname, ScidbTileInterface sti, int storagemax) throws Exception {
		this(tileNamePrefix,arrayname,sti);
		this.storagemax = storagemax;
	}
	
	public synchronized void setStorageMax(int newmax) {
		this.clear();
		this.storagemax = newmax;
	}
	
	public synchronized int getStorageMax() {
		return storagemax;
	}
	
	public synchronized int freeSpace() {
		return storagemax - this.timeMap.size();
	}

	@Override
	public synchronized boolean peek(TileKey id) {
		return this.timeMap.containsKey(id);
	}

	@Override
	public synchronized NiceTile getTile(TileKey id) {
		//return sti.getNiceTile(id);
		NiceTile t = new NiceTile();
		t.id = id;
		if(peek(id)) {
			sti.getStoredTile(arrayname, t);
		} else {
			try {
				sti.getSimulatedBuildTile(arrayname, t);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				t = null;
			}
		}
		return t;
	}

	@Override
	public synchronized Set<TileKey> getAllTileKeys() {
		return timeMap.keySet();
	}
	
	@Override
	public synchronized int tileCount() {
		return this.timeMap.size();
	}
	
	@Override
	public synchronized void clear() {
		lruQueue.clear();
		timeMap.clear();
		this.size = 0;
	}

	@Override
	public synchronized void insertTile(NiceTile tile) {
		TileKey id = tile.id;
		if(!peek(id)) {
			// make room for new tile in storage
			while(this.size >= this.storagemax) {
				this.remove_lru_tile();
			}
			insert_time_pair(tile.id);
			// insert new tile into storage
			if(!this.isBuilt.containsKey(id)) {
				this.sti.buildAndStoreTile(this.arrayname, tile.id);
				this.isBuilt.put(id, true);
			}
		} else { // tile already exists
			// update metadata
			this.update_time_pair(id);
		}

	}

	@Override
	public synchronized void removeTile(TileKey id) {
		if(peek(id)) {
			this.remove_tile(id);
		}

	}
	
	@Override
	public synchronized void touchTile(TileKey id) {
		if(peek(id)) {
			// update metadata
			this.update_time_pair(id);
		}
	}
	
	// find all tiles already cooked in the database
	protected synchronized void findExistingTiles() {
		// get the list of tiles stored in the database
		List<String> arrayNames = sti.getArrayNames();
		for(String arrayName : arrayNames) {
			arrayName = arrayName.replace("\"", "");
			if((arrayName.length() > this.tileNamePrefix.length()) &&
				arrayName.substring(0,this.tileNamePrefix.length()).equals(this.tileNamePrefix)) {
				String[] tokens = arrayName.split("_");
				int zoom = Integer.parseInt(tokens[1]);
				int x = Integer.parseInt(tokens[2]);
				int y = Integer.parseInt(tokens[3]);
				TileKey id = new TileKey(new int[]{x,y}, zoom);
				//this.insert_time_pair(id);
				this.isBuilt.put(id, true);
				//System.out.println(arrayName);
			}
		}
	}
	
	// updates eviction metadata for existing tile id
	protected synchronized void update_time_pair(TileKey id) {
		TimePair tp;
		if(timeMap.containsKey(id)) { // time map tells us if file exists
			tp = timeMap.get(id);
			lruQueue.remove(tp);
			tp.updateTimestamp();
			lruQueue.add(tp);
		}
	}
	
	// adds new eviction metadata for given tile id
	protected synchronized void insert_time_pair(TileKey id) {
		TimePair tp;
		if(!timeMap.containsKey(id)) {
			tp = new TimePair(id,1);
			lruQueue.add(tp);
			timeMap.put(id, tp);
		} else {
			update_time_pair(id);
		}
	}
	
	// removes eviction metadata for existing tile id
	protected synchronized void remove_time_pair(TileKey id) {
		TimePair tp;
		if(timeMap.containsKey(id)) {
			tp = timeMap.get(id);
			lruQueue.remove(tp);
			timeMap.remove(id);
		}
	}
	
	// checks priority queue and removes lru tile
	protected synchronized void remove_lru_tile() {
		// identify least recently used tile
		// will be removed from lru queue in remove function
		TimePair tp = lruQueue.peek();
		if(tp != null) {
			TileKey toremove = tp.getTileKey();
			//System.out.println("removing tile from disk based cache: " + toremove);
			// remove tile from storage
			this.remove_tile(toremove);
		}
	}
	
	// removes a specific tile from buffer
	protected synchronized void remove_tile(TileKey id) {
		if(peek(id)) {
			//NiceTilePacker.removeNiceTile(id);
			// remove metadata
			this.remove_time_pair(id);
			this.size --;
		}
	}
	
	public static void main(String[] args) throws Exception {
		ScidbTileInterface sti = new ScidbTileInterface();
		PreCompNiceTileBuffer buf = new PreCompNiceTileBuffer(sti); // calls init in constructor
		System.out.println(buf.size);
		System.out.println(buf.isBuilt.size());
	}
}
