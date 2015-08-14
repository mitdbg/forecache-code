package abstraction;

import configurations.DBConnector;
import backend.util.NiceTile;

public abstract class NewTileInterface {
	protected DBConnector connectionType;
	
	public NewTileInterface(DBConnector connectionType) {
		this.connectionType = connectionType;
	}
	
	// if user doesn't define a tile structure, get the default
	public abstract TileStructure buildDefaultTileStructure(View v);
	
	// generates the name of the particular zoom level
	public String getZoomLevelName(View v, TileStructure ts, int zoom) {
		StringBuilder sb = new StringBuilder();
		sb.append(v.getName());
		int[] aggWindow = ts.aggregationWindows[zoom];
		for(int i = 0; i < aggWindow.length; i++) {
			sb.append("_").append(aggWindow[i]);
		}
		return sb.toString();
	}
	
	// generates the name of the particular tile, given a view name and tile structure
	public String getTileName(View v,TileStructure ts, NewTileKey id) {
		return getZoomLevelName(v,ts,id.zoom) + "_"+ id.buildTileStringForFile();
	}
	
	// just a generic function to execute a query on the DBMS, and ignore the query output
	public abstract boolean executeQuery(String query);
	
	
	/************* For Precomputation ***************/
	// executes a query to build and store the particular zoom level
	public abstract boolean buildZoomLevel(View v, TileStructure ts, int zoomLevel);
	
	// executes a query on the dbms to build and store the tile 
	public abstract boolean buildTile(String query, TileStructure ts, NiceTile tile);
	
	// retrieve previously stored tile from dbms
	public abstract boolean retrieveTile(String query, TileStructure ts, NiceTile tile);
}
