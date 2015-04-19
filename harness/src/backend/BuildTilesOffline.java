package backend;

import utils.DBInterface;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.disk.VerticaTileInterface;
import backend.util.NiceTile;
import backend.util.TileKey;

/*
 * Used to build and store tiles directly in SciDB
 */
public class BuildTilesOffline {
	public static void main(String[] args) throws Exception {
		System.out.println("populating disk buffer");
		//ScidbTileInterface sti = new ScidbTileInterface();
		VerticaTileInterface vti = new VerticaTileInterface();
		DiskNiceTileBuffer diskbuf = new DiskNiceTileBuffer(DBInterface.nice_tile_cache_dir,DBInterface.hashed_query,DBInterface.threshold);
		
		System.out.println("done populating buffer... building Vertica tiles");
		buildVerticaTiles(diskbuf,vti);
		
		//System.out.println("done populating buffer... building SciDB tiles");
		//buildScidbTiles(diskbuf,sti);

	}
	
	public static void buildVerticaTiles(DiskNiceTileBuffer buffer, VerticaTileInterface vti) {
		// build every tile in SciDB and save it
		for(TileKey id : buffer.getAllTileKeys()) {
			vti.removeStoredTile(DBInterface.arrayname, id);
			NiceTile t = new NiceTile();
			t.id = id;
			vti.buildAndStoreTile(DBInterface.arrayname, id);
			vti.getStoredTile(DBInterface.arrayname, t);
			System.out.println("size: "+t.getSize());
		}
	}
	
	public static void buildScidbTiles(DiskNiceTileBuffer buffer, ScidbTileInterface sti) {
		// build every tile in SciDB and save it
		for(TileKey id : buffer.getAllTileKeys()) {
			sti.removeStoredTile(DBInterface.arrayname, id);
			NiceTile t = new NiceTile();
			t.id = id;
			sti.buildAndStoreTile(DBInterface.arrayname, id);
			sti.getStoredTile(DBInterface.arrayname, t);
			System.out.println("size: "+t.getSize());
		}
	}
}
