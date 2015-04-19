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
		Params p = this.paramsMap.getParams(id);
		executeQuery(DBInterface.arrayname,p,tile);

		NiceTilePacker.writeNiceTile(tile);
		return tile;
	}
	
	public static String getStoredTileName(String arrayname, TileKey id) {
		return arrayname+"_"+id.buildTileStringForFile();
	}
	
	public abstract void executeQuery(String tablename, Params p, NiceTile tile);
}
