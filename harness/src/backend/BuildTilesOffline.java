package backend;

import utils.DBInterface;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.util.TileKey;

/*
 * Used to build and store tiles directly in SciDB
 */
public class BuildTilesOffline {
	public static void main(String[] args) throws Exception {
		System.out.println("populating disk buffer");
		ScidbTileInterface sti = new ScidbTileInterface();
		DiskNiceTileBuffer diskbuf = new DiskNiceTileBuffer(DBInterface.nice_tile_cache_dir,DBInterface.hashed_query,DBInterface.threshold);
		System.out.println("done populating buffer... building tiles");
		buildTiles(diskbuf,sti);

	}
	
	public static void buildTiles(DiskNiceTileBuffer buffer, ScidbTileInterface sti) {
		// build every tile in SciDB and save it
		for(TileKey id : buffer.getAllTileKeys()) {
			//sti.removeStoredTile(DBInterface.arrayname, id);
			sti.buildAndStoreTile(DBInterface.arrayname, id);
		}
	}
}
