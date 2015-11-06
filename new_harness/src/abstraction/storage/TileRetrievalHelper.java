package abstraction.storage;

import abstraction.structures.NewTileKey;
import abstraction.tile.ColumnBasedNiceTile;

// sole purpose is to make fetching tiles faster for metadata purposes
public class TileRetrievalHelper {
	protected String dirpath;
	
	public TileRetrievalHelper(String dirpath) {
		this.dirpath = dirpath;
	}
	
	public ColumnBasedNiceTile getTile(NewTileKey key) {
		return NiceTilePacker.readNiceTile(key,this.dirpath);
	}
	
	public void saveTile(ColumnBasedNiceTile tile) {
		NiceTilePacker.writeNiceTile(tile,this.dirpath);
	}
}
