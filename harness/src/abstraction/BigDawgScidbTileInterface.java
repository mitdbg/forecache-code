package abstraction;

import java.util.ArrayList;
import java.util.List;

import backend.util.NiceTile;

public class BigDawgScidbTileInterface extends Scidb14_12TileInterface {
	
	/************* For Precomputation ***************/
	@Override
	public boolean buildZoomLevel(View v, TileStructure ts, int zoomLevel) {
		String query = generateRetrieveZoomLevelQuery(v,ts,zoomLevel);
		String storeQuery = generateStoreQuery(getZoomLevelName(v,ts,zoomLevel),query);
		String bdsq = addBigDawgWrapper(storeQuery); // wrap in big dawg syntax
		return executeQuery(bdsq);
	}
	
	@Override
	public boolean buildTile(View v, TileStructure ts, ColumnBasedNiceTile tile, NewTileKey id) {
		String query = generateRetrieveTileQuery(v,ts,id);
		String storeQuery = generateStoreQuery(getTileName(v,ts,id),query);
		String bdsq = addBigDawgWrapper(storeQuery); // wrap in big dawg syntax
		return getTile(bdsq,tile);
	}

	@Override
	public boolean retrieveStoredTile(View v, TileStructure ts, ColumnBasedNiceTile tile, NewTileKey id) {
		String query = generateRetrieveTileQuery(v,ts,id);
		String bdq = addBigDawgWrapper(query);
		return getTile(bdq,tile);
	}
	
	@Override
	public List<String> getQueryDataTypes(String query) {
		String showQuery = generateShowQuery(query);
		ColumnBasedNiceTile t = new ColumnBasedNiceTile();
		String bdq = addBigDawgWrapper(showQuery);
		getTile(bdq,t);
		return new ArrayList<String>();
	}
	
	/************* Helper Functions ***************/
	
	// wrap SciDB queries in "ARRAY(?)" syntax
	public String addBigDawgWrapper(String query) {
		return "ARRAY("+query+")";
	}
}
