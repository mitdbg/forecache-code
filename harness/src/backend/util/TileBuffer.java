package backend.util;

import java.util.List;
import java.util.Set;


public interface TileBuffer {
	// is this tile currently located in storage?
	public boolean peek(TileKey id);
	
	// retrieve a tile from storage
	public Tile getTile(TileKey id);
	
	// get a list of the keys for all tiles currently in storage
	public Set<TileKey> getAllTileKeys();
	
	// tell buffer to insert a tile into storage
	public void insertTile(Tile tile);
	
	// tell buffer to remove a tile from storage
	public void removeTile(TileKey id);
	
	// tell buffer to update lru information for tile
	public void touchTile(Tile tile);
}
