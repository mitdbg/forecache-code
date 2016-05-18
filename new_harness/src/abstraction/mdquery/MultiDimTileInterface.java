package abstraction.mdquery;

import java.util.List;
import java.util.Map;


import abstraction.enums.DBConnector;
import abstraction.mdstructures.MultiDimTileKey;
import abstraction.mdstructures.MultiDimTileStructure;
import abstraction.structures.View;
import abstraction.tile.MultiDimColumnBasedNiceTile;


public abstract class MultiDimTileInterface {
	protected DBConnector connectionType;
	
	public MultiDimTileInterface(DBConnector connectionType) {
		this.connectionType = connectionType;
	}
	
	// if user doesn't define a tile structure, get the default
	public abstract MultiDimTileStructure buildDefaultTileStructure(View v);
	
	public abstract MultiDimTileStructure buildTileStructure(View v, int aggregationWindow,
			int tileWidth, int zoomLevels);
	
	// generates the name of the particular zoom level
	public String getZoomLevelName(View v, MultiDimTileStructure ts, int[] zoomPos) {
		StringBuilder sb = new StringBuilder();
		sb.append(v.getName());
		int[] aggWindow = ts.getAggregationWindow(zoomPos);
		for(int i = 0; i < aggWindow.length; i++) {
			sb.append("_").append(aggWindow[i]);
		}
		return sb.toString();
	}
	
	// generates the name of the particular tile, given a view name and tile structure
	public String getTileName(View v,MultiDimTileStructure ts, MultiDimTileKey id) {
		return getZoomLevelName(v,ts,id.zoom) + "_"+ id.buildTileStringForFile();
	}
	
	// just a generic function to execute a query on the DBMS, and ignore the query output
	public abstract boolean executeQuery(String query);
	
	// just a generic function to execute a query on the DBMS, and retrieve the output
	// assumes that data types are passed in the tile object, if available
	// otherwise, issues an additional query to get data types first
	public abstract boolean getTile(String query, MultiDimColumnBasedNiceTile tile);
	
	public abstract boolean getTile(View v, MultiDimTileStructure ts, MultiDimColumnBasedNiceTile tile);
	
	// just a generic function to execute a query on the DBMS, and retrieve the output
	// assumes all data types are strings
	public abstract boolean getRawTile(String query, MultiDimColumnBasedNiceTile tile);
	
	public abstract String getRawData(String query);
	
	public abstract List<String> getQueryDataTypes(String query);
	
	public abstract DimensionBoundary getDimensionBoundaries(String query);
	
	public abstract Class<?> getColumnTypeInJava(String typeName);
	
	/************* For Precomputation ***************/
	
	public abstract boolean checkZoomLevel(View v, MultiDimTileStructure ts, int[] zoomPos);
	public abstract boolean checkTile(View v, MultiDimTileStructure ts, MultiDimTileKey id);
	
	// executes a query to build and store the particular zoom level
	public abstract boolean buildZoomLevel(View v, MultiDimTileStructure ts, int[] zoomPos);
	
	// executes a query on the dbms to build and store the tile 
	public abstract boolean buildTile(View v, MultiDimTileStructure ts, MultiDimColumnBasedNiceTile tile, MultiDimTileKey id);
	
	// retrieve previously stored tile from dbms
	public abstract boolean retrieveStoredTile(View v, MultiDimTileStructure ts, MultiDimColumnBasedNiceTile tile, MultiDimTileKey id);

	public abstract boolean retrieveTileFromStoredZoomLevel(View v, MultiDimTileStructure ts, MultiDimColumnBasedNiceTile tile, MultiDimTileKey id);

	
	/****************** Nested Classes *******************/
	public static class DimensionBoundary {
		public List<String> dimensions;
		public Map<String,List<Integer>> boundaryMap;
	}
}
