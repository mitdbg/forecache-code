package backend.disk;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import utils.DBInterface;

import backend.util.NiceTile;
import backend.util.NiceTileBuffer;
import backend.util.TileKey;
import backend.util.TimePair;

public class DiskNiceTileBuffer implements NiceTileBuffer {
	protected String cache_root_dir;
	protected String threshold;
	protected String hashed_query;
	
	private Map<TileKey,TimePair> timeMap; // for finding things in the queue
	private PriorityQueue<TimePair> lruQueue; // for identifying lru tiles in storage
	private long storagemax;
	private long size;
	private final long DEFAULTMAX = 30304315430L; // default buffer size
	private final int initqueuesize = 50;
	
	public DiskNiceTileBuffer(String cache_root_dir, String hashed_query, String threshold) throws Exception {
		File check = new File(cache_root_dir);
		if(!check.isDirectory()) {
			throw new Exception("cache root directory '"+cache_root_dir+"' is not a directory!");
		}
		
		this.cache_root_dir = cache_root_dir;
		this.threshold = threshold;
		this.hashed_query = hashed_query;
		
		this.lruQueue = new PriorityQueue<TimePair>(this.initqueuesize,new TimePair.TPSort());
		this.timeMap = new HashMap<TileKey,TimePair>();
		this.size = 0;
		this.storagemax = this.DEFAULTMAX;
		init(); // check cache root for existing tiles
	}
	
	public DiskNiceTileBuffer(String cache_root_dir, String hashed_query, String threshold, int storagemax) throws Exception {
		File check = new File(cache_root_dir);
		if(!check.isDirectory()) {
			throw new Exception("cache root directory '"+cache_root_dir+"' is not a directory!");
		}
		
		this.cache_root_dir = cache_root_dir;
		this.threshold = threshold;
		this.hashed_query = hashed_query;
		
		this.lruQueue = new PriorityQueue<TimePair>(this.initqueuesize,new TimePair.TPSort());
		timeMap = new HashMap<TileKey,TimePair>();
		this.size = 0;
		this.storagemax = storagemax;
		init(); // check cache root for existing tiles
	}
	
	public synchronized void setStorageMax(int newmax) {
		this.clear();
		this.storagemax = newmax;
	}

	@Override
	public synchronized boolean peek(TileKey id) {
		return this.timeMap.containsKey(id);
	}

	@Override
	public synchronized NiceTile getTile(TileKey id) {
		return NiceTilePacker.readNiceTile(id);
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
		if(this.storagemax == 0) return;
		TileKey id = tile.id;
		if(!peek(id)) {
			// make room for new tile in storage
			while(this.size >= this.storagemax) {
				this.remove_lru_tile();
			}
			// insert new tile into storage
			NiceTilePacker.writeNiceTile(tile);
		} else { // tile already exists
			// update metadata
			this.update_time_pair(id);
		}

	}

	@Override
	public synchronized void removeTile(TileKey id) {
		if(peek(id)) {
			timeMap.get(id);
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
	
	// find all tiles in cache directory
	protected synchronized void init() {
		//String rootpath = build_start_path();
		//File root = new File(rootpath);
		File root = new File(cache_root_dir);
		init_helper(root, 0);
	}
	
	private synchronized int init_helper(File node, int currsize) {
		if(node.isDirectory()) {
			File[] children = node.listFiles();
			for(File file : children) { // iterate on children
				if(currsize < this.storagemax) {
					currsize = init_helper(file, currsize);
				} else {
					break;
				}
			}
		} else if (node.isFile() && node.canRead()){ // add file to cache
			NiceTile t = NiceTilePacker.readNiceTileDefault(node);
			if(t != null) {
				if((currsize+1) <= this.storagemax) {
					insert_time_pair(t.id); // add to lru metadata
					currsize++;
					//System.out.println("using " + currsize + " bytes out of " + storagemax);
					System.out.println("inserting tile in disk based cache: '"+t.id+"'");
				}
			}
		}
		return currsize;
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
	
	private synchronized String build_start_path() {
		StringBuilder path = new StringBuilder();
		path.append(cache_root_dir).append("/").append(hashed_query).append("/")
		.append(threshold);
		//System.out.println("root path: '"+path+"'");
		return path.toString();
	}
	
	private synchronized String build_tile_filepath(TileKey id) {
		StringBuilder path = new StringBuilder();
		String tile_hash = get_tile_hash(id);
		if(tile_hash.length() == 0) {
			//System.out.println("tile hash length is 0");
			return null;
		}
		path.append(cache_root_dir).append("/").append(hashed_query).append("/")
		.append(threshold).append("/").append(id.zoom).append("/")
			.append(tile_hash);
		//System.out.println("path: '"+path+"'");
		return path.toString();
	}
	
	private synchronized String get_tile_hash(TileKey id) {
		String tile_id = id.buildTileString();
		if(tile_id == null) {
			return null;
		}
		return DBInterface.getTileHash(tile_id);
	}

}
