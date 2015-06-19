package backend.disk;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import utils.DBInterface;

import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.TileKey;
import backend.util.TimePair;

// used to keep track of user's last x moves, where x=bufferSize
public class PreCompNiceTileLruBuffer implements NiceTileBuffer {
	protected Queue<TileKey> q;
	public static int defaultSize = 4;

	protected Map<TileKey,Boolean> contains;
	protected Map<TileKey,Boolean> isBuilt;
	protected int storagemax;
	protected final int DEFAULTMAX = 1; // default buffer size
	protected final int initqueuesize = 50;
	protected String tileNamePrefix = DBInterface.arrayname+"_"; // default prefix
	protected String arrayname = DBInterface.arrayname;
	protected ScidbTileInterface sti;

	
	/* constructors assuming we need to initialize isBuilt object */
	public PreCompNiceTileLruBuffer(ScidbTileInterface sti) throws Exception {
		this.sti = sti;
		
		this.isBuilt = new HashMap<TileKey,Boolean>();
		q = new LinkedList<TileKey>();
		this.storagemax = this.DEFAULTMAX;
		this.contains = new HashMap<TileKey,Boolean>();
		findExistingTiles(); // initialize isBuilt object
	}
	
	public PreCompNiceTileLruBuffer(ScidbTileInterface sti, int storagemax) throws Exception {
		this(sti);
		this.storagemax = storagemax;
	}
	
	public PreCompNiceTileLruBuffer(String tileNamePrefix, String arrayname, ScidbTileInterface sti) throws Exception {
		this(sti);
		this.tileNamePrefix = tileNamePrefix;
		this.arrayname = arrayname;
	}
	
	public PreCompNiceTileLruBuffer(String tileNamePrefix, String arrayname, ScidbTileInterface sti,
			int storagemax) throws Exception {
		this(tileNamePrefix,arrayname,sti);
		this.storagemax = storagemax;
	}
	
	/* constructors for when we don't need to initialize isBuilt object */
	
	public PreCompNiceTileLruBuffer(ScidbTileInterface sti,
			Map<TileKey,Boolean> isBuilt) throws Exception {
		this.sti = sti;
		this.isBuilt = isBuilt;
		
		this.storagemax = this.DEFAULTMAX;
		q = new LinkedList<TileKey>();
		this.contains = new HashMap<TileKey,Boolean>();
	}
	
	public PreCompNiceTileLruBuffer(ScidbTileInterface sti, int storagemax,
			Map<TileKey,Boolean> isBuilt) throws Exception {
		this(sti,isBuilt);
		this.storagemax = storagemax;
	}
	
	public PreCompNiceTileLruBuffer(String tileNamePrefix, String arrayname, ScidbTileInterface sti,
			Map<TileKey,Boolean> isBuilt) throws Exception {
		this(sti,isBuilt);
		
		this.tileNamePrefix = tileNamePrefix;
		this.arrayname = arrayname;
	}
	
	PreCompNiceTileLruBuffer(String tileNamePrefix, String arrayname, ScidbTileInterface sti,
			int storagemax, Map<TileKey,Boolean> isBuilt) throws Exception {
		this(tileNamePrefix,arrayname,sti,isBuilt);
		this.storagemax = storagemax;
	}
	
	/* end constructors */
	
	public synchronized void setStorageMax(int newmax) {
		this.clear();
		this.storagemax = newmax;
	}
	
	public synchronized int getStorageMax() {
		return storagemax;
	}
	
	public synchronized int freeSpace() {
		return storagemax - contains.size();
	}

	@Override
	public synchronized boolean peek(TileKey id) {
		return this.contains.containsKey(id);
	}

	@Override
	public synchronized NiceTile getTile(TileKey id) {
		if(peek(id)) {
			q.remove(id);
			q.add(id);
			NiceTile t = new NiceTile();
			t.id = id;
			sti.getStoredTile(arrayname, t);
			return t;
		}
		return null;
	}

	@Override
	public synchronized Set<TileKey> getAllTileKeys() {
		return contains.keySet();
	}
	
	@Override
	public synchronized int tileCount() {
		return this.contains.size();
	}
	
	@Override
	public synchronized void clear() {
		contains.clear();
		q.clear();
	}

	@Override
	public synchronized void insertTile(NiceTile tile) {
		if(this.storagemax == 0) return;
		TileKey id = tile.id;
		if(!peek(id)) {
			// make room for new tile in storage
			while(contains.size() >= this.storagemax) {
				dequeue();
			}
			enqueue(tile);
			// insert new tile into storage, if necessary
			if(!this.isBuilt.containsKey(id)) {
				this.sti.buildAndStoreTile(this.arrayname, tile.id);
				this.isBuilt.put(id, true);
			}
		} else { // tile already exists
			// update metadata
			touchTile(id);
		}

	}

	@Override
	public synchronized void removeTile(TileKey id) {
		q.remove(id);
		contains.remove(id);
	}
	
	@Override
	public synchronized void touchTile(TileKey id) {
		if(peek(id)) {
			// update metadata
			q.remove(id);
			q.add(id);
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
				String[] tokens = arrayName.substring(this.tileNamePrefix.length()).split("_");
				int zoom = Integer.parseInt(tokens[0]);
				int x = Integer.parseInt(tokens[1]);
				int y = Integer.parseInt(tokens[2]);
				TileKey id = new TileKey(new int[]{x,y}, zoom);
				//this.insert_time_pair(id);
				this.isBuilt.put(id, true);
				//System.out.println(arrayName);
			}
		}
	}
	
	protected synchronized void enqueue(NiceTile t) {
		q.remove(t.id);
		contains.put(t.id, true);
		q.add(t.id);
	}
	
	// remove first item from queue
	protected synchronized void dequeue() {
		TileKey toRemove = q.remove();
		contains.remove(toRemove);
		//NiceTile t = new NiceTile();
		//t.id = toRemove;
		//sti.getStoredTile(arrayname, t);
		//return t;
	}
}
