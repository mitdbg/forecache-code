package backend.disk;

import java.util.Map;

import utils.DBInterface;
import backend.util.NiceTile;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.TileKey;

public abstract class TileInterface {
	protected ParamsMap paramsMap;
	protected String paramsfile;
	protected String delim;
	
	
	public TileInterface() {
		this(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}
	
	public TileInterface(String paramsfile, String delim) {
		this.paramsfile = paramsfile;
		this.delim = delim;
		this.paramsMap = new ParamsMap(this.paramsfile,this.delim);
	}
	
	public synchronized NiceTile getNiceTile(TileKey id) {
		NiceTile tile = NiceTilePacker.readNiceTile(id);
		if(tile != null) return tile;
		
		tile = new NiceTile(id);
		String tile_id = id.buildTileString();
		if(tile_id == null) {
			System.out.println("could not build tile_id");
			return null;
		}
		Map<Integer,Params> map1 = this.paramsMap.get(id.buildTileString());
		if(map1 == null) {
			System.out.println("map1 is null");
			return null;
		}
		Params p = map1.get(id.zoom);
		if(p == null) {
			System.out.println("params is null");
			return null;
		}
		executeQuery(DBInterface.arrayname,p,tile);

		NiceTilePacker.writeNiceTile(tile);
		return tile;
	}
	
	public abstract void executeQuery(String tablename, Params p, NiceTile tile);
}
