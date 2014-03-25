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
	private final int storagemax;
	private int size;
	private final int DEFAULTMAX = 100000000; // default buffer size
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
		timeMap = new HashMap<TileKey,TimePair>();
		this.size = 0;
		this.storagemax = this.DEFAULTMAX;
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
	}

	@Override
	public synchronized boolean peek(TileKey id) {
		String path = build_tile_filepath(id);
		if(path == null) {
			return false;
		}
		File filepath = new File(path);
		return filepath.isFile() && filepath.canRead();
	}

	@Override
	public synchronized Tile getTile(TileKey id) {
		if(!peek(id)) {
			return null;
		}
		String path = build_tile_filepath(id);
		try {
			File filepath = new File(path);
			byte[] data = Files.toByteArray(filepath);
			return new Tile(id,data);
		} catch (IOException e) {
			System.out.println("error occured while retrieving data from disk");
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public synchronized List<Tile> getAllTiles() {
		// DON'T USE THIS!!!!
		return null;
	}

	@Override
	public synchronized Set<TileKey> getAllTileKeys() {
		return timeMap.keySet();
	}

	@Override
	public synchronized void insertTile(Tile tile) {
		// TODO Auto-generated method stub

	}

	@Override
	public synchronized void removeTile(TileKey id) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public synchronized void touchTile(Tile tile) {
		// TODO Auto-generated method stub
		
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
		String tile_id = build_tile_string(id);
		if(tile_id == null) {
			return null;
		}
		return DBInterface.getTileHash(tile_id);
	}
	
	private String build_tile_string(TileKey id) {
		StringBuffer tile_id = new StringBuffer();
		tile_id.append("[");
		ImmutableList<Integer> l = id.getID();
		if(l.size() > 0) {
			tile_id.append(l.get(0));
		} else {
			return null;
		}
		for(int i = 1; i < l.size(); i++) {
			tile_id.append(", ").append(l.get(i));
		}
		tile_id.append("]");
		return tile_id.toString();
	}

}
