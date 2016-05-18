package abstraction.mdstorage;

import abstraction.mdstructures.MultiDimTileKey;
import abstraction.tile.MultiDimColumnBasedNiceTile;

// sole purpose is to make fetching tiles faster for metadata purposes
public class MultiDimTileRetrievalHelper {
	protected String dirpath;
	
	public MultiDimTileRetrievalHelper(String dirpath) {
		this.dirpath = dirpath;
	}
	
	public MultiDimColumnBasedNiceTile getTile(MultiDimTileKey key) {
		return MultiDimTilePacker.readNiceTile(key,this.dirpath);
	}
	
	public void saveTile(MultiDimColumnBasedNiceTile tile) {
		MultiDimTilePacker.writeNiceTile(tile,this.dirpath);
	}
}
