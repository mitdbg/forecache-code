package abstraction;

import java.util.Iterator;

import backend.util.NiceTile;
import configurations.DBConnector;

public class Scidb14_12TileInterface extends NewTileInterface {
	
	public Scidb14_12TileInterface() {
		super(DBConnector.SCIDB);
	}

	@Override
	public TileStructure buildDefaultTileStructure(View v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean executeQuery(String query) {
		// TODO Auto-generated method stub
		return false;
	}

	/************* For Precomputation ***************/
	
	@Override
	public boolean buildZoomLevel(View v, TileStructure ts, int zoomLevel) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean buildTile(String query, TileStructure ts, NiceTile tile) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retrieveTile(String query, TileStructure ts, NiceTile tile) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	/************* Helper Functions ***************/
	
	// get the SciDB schema for this query
	public void getSchema(View v) {
		
	}
	
	public String generateRetrieveTileQuery(View v, TileStructure ts,NewTileKey id) {
		StringBuilder sb = new StringBuilder();
		sb.append("subarray(").append(getZoomLevelName(v,ts,id.zoom));
		int[] highs = new int[ts.tileWidths.length];
		for(int i = 0; i < ts.tileWidths.length; i++) {
			int low = ts.tileWidths[i]*id.dimIndices[i];
			highs[i] = low + ts.tileWidths[i] - 1;
			sb.append(",").append(low);
		}
		for(int i = 0; i < highs.length; i++) {
			sb.append(",").append(highs[i]);
		}
		sb.append(")");
		return sb.toString();
	}
	
	public String generateZoomLevelQuery(View v, TileStructure ts, int zoom) {
		String query = v.getQuery();
		StringBuilder sb = new StringBuilder();
		sb.append("store(").append("regrid(").append(query);
		int[] aggWindow = ts.aggregationWindows[zoom];
		for(int i = 0; i < aggWindow.length; i++) {
			sb.append(",").append(aggWindow[i]);
		}
		Iterator<String> summaries = v.getSummaryFunctions();
		while(summaries.hasNext()) {
			sb.append(",").append(summaries.next());
		}
		sb.append("),").append(getZoomLevelName(v,ts,zoom)).append(")");
		return sb.toString();
	}
	
}
