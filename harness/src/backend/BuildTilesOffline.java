package backend;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.DBInterface;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.disk.VerticaTileInterface;
import backend.util.NiceTile;
import backend.util.ParamsMap;
import backend.util.TileKey;

/*
 * Used to build and store tiles directly in SciDB
 */
public class BuildTilesOffline {
	public static BufferedWriter log;
	public static String logfile = "tile_build_log.tsv";
	
	public static void main(String[] args) throws Exception {
		try {
			log = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logfile)));
		} catch (IOException e) {
		    System.out.println("Couldn't open logfile");
		    e.printStackTrace();
		    return;
		}  
		
		System.out.println("populating disk buffer");
		ScidbTileInterface sti = new ScidbTileInterface();
		//VerticaTileInterface vti = new VerticaTileInterface();
		DiskNiceTileBuffer diskbuf = new DiskNiceTileBuffer(DBInterface.nice_tile_cache_dir,DBInterface.hashed_query,DBInterface.threshold);
		
		//System.out.println("done populating buffer... building Vertica tiles");
		//buildVerticaTiles(diskbuf,vti);
		
		//System.out.println("done populating buffer... building SciDB tiles");
		//buildAllScidbTiles(diskbuf,sti);
		
		System.out.println("calculating SciDB tile timings...");
		measureScidbTimings(diskbuf,sti,1,1);
		
		try {
			log.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void buildSpecificVerticaTiles(Collection<TileKey> ids, VerticaTileInterface vti, int runs) throws IOException {
		for(TileKey id : ids) {
			for(int i = 0; i < runs; i++) {
				vti.removeStoredTile(DBInterface.arrayname, id);
				NiceTile t = new NiceTile();
				t.id = id;
				long duration = vti.buildAndStoreTile(DBInterface.arrayname, id);
				vti.getStoredTile(DBInterface.arrayname, t);
				System.out.println("size: "+t.getSize());
				
				String report = t.id.zoom + "\t" + t.id.buildTileStringForFile() + "\t" + duration + "\t" + i;
				System.out.println(report);
				log.write(report);
				log.newLine();
			}
			log.flush();
		}
	}
	
	public static void buildAllVerticaTiles(DiskNiceTileBuffer buffer, VerticaTileInterface vti) throws IOException {
		// build every tile in Vertica and save it
		buildSpecificVerticaTiles(buffer.getAllTileKeys(),vti, 1);
	}
	
	public static void buildScidbTile(ScidbTileInterface sti, TileKey id, int run) throws IOException {
		//sti.removeStoredTile(DBInterface.arrayname, id); // for building and storing the tile
		NiceTile t = new NiceTile();
		t.id = id;
		//long duration = sti.buildAndStoreTile(DBInterface.arrayname, id); // for building and storing the tile
		long duration = sti.MeasureTile(DBInterface.arrayname, id); // for measuring execution times
		sti.getStoredTile(DBInterface.arrayname, t);
		System.out.println("duration (ms): "+duration);
		System.out.println("size: "+t.getSize());
		
		String report = t.id.zoom + "\t" + t.id.buildTileStringForFile() + "\t" + duration + "\t" + run;
		System.out.println(report);
		log.write(report);
		log.newLine();
		log.flush();
	}
	
	public static void buildSpecificScidbTiles(Collection<TileKey> ids, ScidbTileInterface sti) throws IOException {
		for(TileKey id : ids) {
			buildScidbTile(sti,id,0);
			//return;
		}
	}
	
	public static void buildAllScidbTiles(DiskNiceTileBuffer buffer, ScidbTileInterface sti) throws IOException {
		// build every tile in SciDB and save it
		buildSpecificScidbTiles(buffer.getAllTileKeys(),sti);
	}
	
	public static void measureScidbTimings(DiskNiceTileBuffer buffer, ScidbTileInterface sti, int runs, int maxtiles) throws IOException {
		ParamsMap paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		int maxzoom = -1;
		Map<Integer,List<TileKey>> tilesPerZoom = new HashMap<Integer,List<TileKey>>();
		// get all keys, partition into zoom levels
		for(TileKey id: paramsMap.allKeysSet) {
			if(maxzoom < id.zoom) maxzoom = id.zoom;
			List<TileKey> temp = tilesPerZoom.get(id.zoom);
			if(temp == null) {
				temp = new ArrayList<TileKey>();
				tilesPerZoom.put(id.zoom,temp);
			}
			if(temp.size() < maxtiles) temp.add(id);
		}
		
		// only bother if there are zoom levels to analyze
		if(maxzoom < 0) return;
		
		// Build X tiles at each zoom level, where X=runs
		// only re-sample if necessary, to ensure an even distribution across X
		for(int zoom = 0; zoom <= maxzoom; zoom++) {
			//System.out.println("zoom:"+zoom);
			List<TileKey> temp = tilesPerZoom.get(zoom);
			int count = temp.size();
			if(count > maxtiles) count = maxtiles;
			int[] runspertile = new int[count];
			int base = runs / count;
			int total = 0;
			for(int i = 0; i < count; i++) {
				runspertile[i] = base;
				total += base;
				if((total + base*(count - i - 1)) < runs) {
					runspertile[i]++;
					total++;
				}
				//System.out.println(runspertile[i]);
			}
			Collections.shuffle(temp);
			for(int i = 0; i < count; i++) {
				TileKey id = temp.get(i);
				for(int j = 0; j < runspertile[i]; j++) {
					buildScidbTile(sti,id,j);
				}
			}
		}
		
	}
}
