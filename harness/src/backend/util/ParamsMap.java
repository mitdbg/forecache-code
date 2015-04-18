package backend.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import utils.UtilityFunctions;

public class ParamsMap {
	public Map<String,Map<Integer,Params>> paramsMap;
	public Map<TileKey,Boolean> allKeys;
	public Set<TileKey> allKeysSet;
	private String paramsfile;
	private String delim;
	
	public ParamsMap(String paramsfile, String delim) {
		this.paramsfile = paramsfile;
		this.delim = delim;
		this.paramsMap = new HashMap<String, Map<Integer, Params>>();
		this.allKeys = new HashMap<TileKey,Boolean>();
		this.allKeysSet = new HashSet<TileKey>();
		parseParamsFile();
	}
	
	public Params getParams(TileKey id) {
		Params p = null;
		String tile_id = id.buildTileString();
		if(tile_id == null) {
			System.out.println("could not build tile_id");
			return p;
		}
		Map<Integer,Params> map1 = get(tile_id);
		if(map1 == null) {
			System.out.println("map1 is null");
			return p;
		}
		p = map1.get(id.zoom);
		if(p == null) {
			System.out.println("params is null");
		}
		return p;
	}
	
	public Map<Integer,Params> get(String tileidstring) {
		return this.paramsMap.get(tileidstring);
	}

	public void parseParamsFile() {
		File pf = new File(this.paramsfile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(pf));
			for(String line; (line = br.readLine()) != null;) {
				String[] tokens = line.split(delim);
				if(tokens.length != 7) {
					System.out.println("Error in params file. line does not have correct number of items!");
					return;
				}
				String tile_id = "";
				int zoom = -1;
				Params p = new Params();
				for(int i = 0; i < tokens.length; i++) {
					switch(i) {
						case 0: tile_id = tokens[i];
								//System.out.println("tile id: '"+tile_id+"'");
								break;
						case 1: zoom = Integer.parseInt(tokens[i]);
								//System.out.println("zoom: "+zoom);
								break;
						case 2: p.xmin = Integer.parseInt(tokens[i]);
								break;
						case 3: p.ymin = Integer.parseInt(tokens[i]);
								break;
						case 4: p.xmax = Integer.parseInt(tokens[i]);
								break;
						case 5: p.ymax = Integer.parseInt(tokens[i]);
								break;
						case 6: p.width = Integer.parseInt(tokens[i]);
								break;
						default:
					}
				}
				if((zoom >= 0) && (tile_id != null) && (tile_id.length() > 0)) {
					//System.out.println("tile id: '"+tile_id+"'");
					//System.out.println("zoom: "+zoom);
					Map<Integer, Params> temp = this.paramsMap.get(tile_id);
					if(temp == null) {
						temp = new HashMap<Integer, Params>();
						this.paramsMap.put(tile_id,temp);
					}
					temp.put(zoom, p);
					
					// this is used to enumerate over all tiles
					this.allKeys.put(new TileKey(UtilityFunctions.parseTileIdInteger(tile_id),zoom), true);
				}
			}
			this.allKeysSet = this.allKeys.keySet();
		} catch (IOException e) {
			System.out.println("error occured while reading params file");
			e.printStackTrace();
		}

	}
}
