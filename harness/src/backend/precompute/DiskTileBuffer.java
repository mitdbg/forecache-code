package backend.precompute;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import utils.DBInterface;
import utils.UtilityFunctions;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import backend.util.Tile;
import backend.util.TileBuffer;
import backend.util.TileKey;
import backend.util.TimePair;

public class DiskTileBuffer implements TileBuffer {
	protected String cache_root_dir;
	protected String threshold;
	protected String hashed_query;
	
	private Map<TileKey,TimePair> timeMap; // for finding things in the queue
	private PriorityQueue<TimePair> lruQueue; // for identifying lru tiles in storage
	private final long storagemax;
	private long size;
	private final long DEFAULTMAX = 16431543; // default buffer size
	private final int initqueuesize = 50;
	
	public DiskTileBuffer(String cache_root_dir, String hashed_query, String threshold) throws Exception {
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
	
	public DiskTileBuffer(String cache_root_dir, String hashed_query, String threshold, int storagemax) throws Exception {
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

	@Override
	public synchronized boolean peek(TileKey id) {
		return this.timeMap.containsKey(id);
	}

	@Override
	public synchronized Tile getTile(TileKey id) {
		Tile myresult = null;
		if(!peek(id)) { // not recorded in cache
			return myresult;
		}
		String pathname = build_tile_filepath(id);
		if(pathname == null) { // could not get path
			return myresult;
		}
		try {
			File filepath = new File(pathname);
			byte[] data = Files.toByteArray(filepath);
			myresult = new Tile(id,data);
		} catch (IOException e) {
			System.out.println("error occured while retrieving data from disk");
			e.printStackTrace();
		}
		return myresult;
	}

	@Override
	public synchronized Set<TileKey> getAllTileKeys() {
		return timeMap.keySet();
	}

	@Override
	public synchronized void insertTile(Tile tile) {
		TileKey id = tile.getTileKey();
		if(!peek(id)) {
			int tilesize = tile.getDataSize();
			// make room for new tile in storage
			while((this.size + tilesize) > this.storagemax) {
				this.remove_lru_tile();
			}
			// insert new tile into storage
			this.insert_tile(tile);
		} else { // tile already exists
			// update metadata
			this.update_time_pair(id);
		}

	}

	@Override
	public synchronized void removeTile(TileKey id) {
		if(peek(id)) {
			TimePair toremove = timeMap.get(id);
			int tilesize = toremove.getTileSize();
			this.remove_tile(id, tilesize);
		}

	}
	
	@Override
	public synchronized void touchTile(Tile tile) {
		TileKey id = tile.getTileKey();
		if(peek(id)) {
			// update metadata
			this.update_time_pair(id);
		}
	}
	
	// find all tiles in cache directory
	protected synchronized void init() {
		String rootpath = build_start_path();
		File root = new File(rootpath);
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
			File parent = node.getParentFile(); // get zoom level
			int zoom = -1;
			try {
				zoom = Integer.parseInt(parent.getName());
			} catch(NumberFormatException e) {
				System.out.println("error retrieving zoom level for file");
				e.printStackTrace();
			}
			if(zoom < 0) { // couldn't parse int properly
				return currsize;
			}
			String tileidstring = DBInterface.getTileId(node.getName());
			//System.out.println("zoom: " + zoom);
			if(tileidstring.length() == 0) { // couldn't get tile_id from db using hash
				return currsize;
			}
			try {
				byte[] data = Files.toByteArray(node);
				int tilesize = data.length;
				List<Integer> tile_id = UtilityFunctions.parseTileIdInteger(tileidstring);
				TileKey key = new TileKey(tile_id,zoom);
				if((tilesize + currsize) <= this.storagemax) {
					insert_time_pair(key, tilesize); // add to lru metadata
					currsize += tilesize;
					System.out.println("using " + currsize + " bytes out of " + storagemax);
				}
			} catch (IOException e) {
				System.out.println("error occured while retrieving data from disk for cache init");
				e.printStackTrace();
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
	protected synchronized void insert_time_pair(TileKey id, int tilesize) {
		TimePair tp;
		if(!timeMap.containsKey(id)) {
			tp = new TimePair(id, tilesize);
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
	
	// inserts a specific tile into buffer
	protected synchronized void insert_tile(Tile tile) {
		int tilesize = tile.getDataSize();
		TileKey id = tile.getTileKey();
		String pathname = build_tile_filepath(id);
		if(pathname == null) { // could not get path
			return;
		}
		try {
			File filepath = new File(pathname);
			Files.write(tile.getDataCopy(),filepath);
			
			// add metadata for eviction purposes
			this.insert_time_pair(id, tile.getDataSize());
			this.size += tilesize;
		} catch (IOException e) {
			System.out.println("error occured while writing tile to disk");
			e.printStackTrace();
		}
	}
	
	// checks priority queue and removes lru tile
	protected synchronized void remove_lru_tile() {
		// identify least recently used tile
		// will be removed from lru queue in remove function
		TimePair tp = lruQueue.peek();
		if(tp != null) {
			TileKey toremove = tp.getTileKey();
			int tilesize = tp.getTileSize();
			System.out.println("removing tile: " + toremove);
			// remove tile from storage
			this.remove_tile(toremove, tilesize);
		}
	}
	
	// removes a specific tile from buffer
	protected synchronized void remove_tile(TileKey id, int tilesize) {
		if(peek(id)) {
			String pathname = build_tile_filepath(id);
			if(pathname == null) { // could not get path
				return;
			}
			try {
				File filepath = new File(pathname);
				filepath.delete();
				
				// remove metadata
				this.remove_time_pair(id);
				this.size -= tilesize;
			} catch (Exception e) {
				System.out.println("error occured while removing tile from disk");
				e.printStackTrace();
			}
		}
	}
	
	private String build_start_path() {
		StringBuilder path = new StringBuilder();
		path.append(cache_root_dir).append("/").append(hashed_query).append("/")
		.append(threshold);
		//System.out.println("root path: '"+path+"'");
		return path.toString();
	}
	
	private String build_tile_filepath(TileKey id) {
		StringBuilder path = new StringBuilder();
		String tile_hash = get_tile_hash(id);
		if(tile_hash.length() == 0) {
			//System.out.println("tile hash length is 0");
			return null;
		}
		path.append(cache_root_dir).append("/").append(hashed_query).append("/")
		.append(threshold).append("/").append(id.getZoom()).append("/")
			.append(tile_hash);
		//System.out.println("path: '"+path+"'");
		return path.toString();
	}
	
	private String get_tile_hash(TileKey id) {
		String tile_id = id.buildTileString();
		if(tile_id == null) {
			return null;
		}
		return DBInterface.getTileHash(tile_id);
	}

}
