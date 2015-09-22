package abstraction.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import abstraction.util.ColumnBasedNiceTile;
import abstraction.util.NewTileKey;
import abstraction.util.TileStructure;
import abstraction.util.View;

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

	@Override
	public boolean getTile(String query, ColumnBasedNiceTile tile) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean getTile(View v, TileStructure ts, ColumnBasedNiceTile tile) {
		boolean built = false;
		if(!checkZoomLevel(v,ts,tile.id.zoom)) {
			built = buildZoomLevel(v,ts,tile.id.zoom);
		}
		return built && this.retrieveTileFromStoredZoomLevel(v, ts, tile, tile.id);
	}
	
	@Override
	// just a generic function to execute a query on the DBMS, and retrieve the output
	public boolean getRawTile(String query, ColumnBasedNiceTile tile) {
		//TODO fill this in
		return false;
	}
	
	// just used to get the raw data as a single gigantic string
	// probably won't be used with scidb connectors
	@Override
	public String getRawData(String query) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<String> getQueryDataTypes(String query) {
		String showQuery = generateShowQuery(query);
		ColumnBasedNiceTile t = new ColumnBasedNiceTile();
		getRawTile(showQuery,t);
		int schema_index = t.getIndex("schema");
		String schema = (String) t.get(schema_index, 0);
		List<String> dataTypes = parseSchemaForDataTypes(schema);
		return dataTypes;
	}
	
	@Override
	public Map<String,List<Integer>> getDimensionBoundaries(String query) {
		String showQuery = generateShowQuery(query);
		ColumnBasedNiceTile t = new ColumnBasedNiceTile();
		getRawTile(showQuery,t);
		int schema_index = t.getIndex("schema");
		String schema = (String) t.get(schema_index, 0);
		Map<String,List<Integer>> boundaryMap = parseSchemaForDimensionBoundaries(schema);
		return boundaryMap;
	}
	
	@Override
	public Class<?> getColumnTypeInJava(String typeName) {
		if(typeName.equals("bool")) return Boolean.class;
		else if (typeName.equals("string")) return String.class;
		else if(typeName.equals("char")) return Character.class;
		//else if(typeName.equals("datetime")) return String.class;
		else if (typeName.equals("double")) return Double.class;
		else if (typeName.equals("float")) return Float.class;
		else if (typeName.substring(0,3).equals("int")) return Integer.class;
		else if (typeName.substring(0,4).equals("uint")) return Long.class;
		else return String.class;
	}
	

	/************* For Precomputation ***************/

	//is this zoom level already built?
	@Override
	public boolean checkZoomLevel(View v, TileStructure ts, int zoomLevel) {
		String query = getZoomLevelName(v,ts,zoomLevel);
		String showQuery = generateShowQueryForArray(query);
		return executeQuery(showQuery);
	}
	
	@Override
	public boolean buildZoomLevel(View v, TileStructure ts, int zoomLevel) {
		String query = generateBuildZoomLevelQuery(v,ts,zoomLevel);
		String storeQuery = generateStoreQuery(getZoomLevelName(v,ts,zoomLevel),query);
		return executeQuery(storeQuery);
	}
	
	@Override
	public boolean checkTile(View v, TileStructure ts, NewTileKey id) {
		String query = getTileName(v,ts,id);
		String showQuery = generateShowQueryForArray(query);
		return executeQuery(showQuery);
	}

	@Override
	public boolean buildTile(View v, TileStructure ts, ColumnBasedNiceTile tile, NewTileKey id) {
		String query = generateBuildTileQuery(v,ts,id);
		String storeQuery = generateStoreQuery(getTileName(v,ts,id),query);
		return getTile(storeQuery,tile);
	}

	@Override
	public boolean retrieveStoredTile(View v, TileStructure ts, ColumnBasedNiceTile tile, NewTileKey id) {
		String tileName = getTileName(v,ts,id);
		String query = this.generateScanQueryForArray(tileName);
		return getTile(query,tile);
	}
	
	@Override
	public boolean retrieveTileFromStoredZoomLevel(View v,
			TileStructure ts, ColumnBasedNiceTile tile, NewTileKey id) {
		String query = generateRetrieveTileQuery(v,ts,id);
		return getTile(query,tile);
	}
	
	
	/************* Helper Functions ***************/
	
	// does not assume that zoom level is built
	protected String generateBuildTileQuery(View v, TileStructure ts,NewTileKey id) {
		String query = generateBuildZoomLevelQuery(v,ts,id.zoom);
		int[] highs = new int[ts.tileWidths.length];
		int[] lows = new int[ts.tileWidths.length];
		for(int i = 0; i < ts.tileWidths.length; i++) {
			lows[i] = ts.tileWidths[i]*id.dimIndices[i];
			highs[i] = lows[i] + ts.tileWidths[i] - 1;
		}
		return generateSubarrayQuery(query,lows,highs);
	}
	
	// assumes zoom level is already built
	protected String generateRetrieveTileQuery(View v, TileStructure ts,NewTileKey id) {
		String query = getZoomLevelName(v,ts,id.zoom);
		int[] highs = new int[ts.tileWidths.length];
		int[] lows = new int[ts.tileWidths.length];
		for(int i = 0; i < ts.tileWidths.length; i++) {
			lows[i] = ts.tileWidths[i]*id.dimIndices[i];
			highs[i] = lows[i] + ts.tileWidths[i] - 1;
		}
		return generateSubarrayQuery(query,lows,highs);
	}
	
	// builds, but does not store, the zoom level
	protected String generateBuildZoomLevelQuery(View v, TileStructure ts, int zoom) {
		String query = v.getQuery();
		int[] aggWindow = ts.aggregationWindows[zoom];
		Iterator<String> summaryFunctions = v.getSummaryFunctions();
		return generateRegridQuery(query,aggWindow,summaryFunctions);
	}
	
	protected String generateSubarrayQuery(String query, int[] lows, int[] highs) {
		StringBuilder sb = new StringBuilder();
		sb.append("subarray(").append(query);
		for(int i = 0; i < lows.length; i++) {
			sb.append(",").append(lows[i]);
		}
		for(int i = 0; i < highs.length; i++) {
			sb.append(",").append(highs[i]);
		}
		sb.append(")");
		return sb.toString();
	}
	
	// execute SciDB regrid statement given the aggregatino windows and summary functions
	protected String generateRegridQuery(String query, int[] aggWindow, Iterator<String> summaryFunctions) {
		StringBuilder sb = new StringBuilder();
		sb.append("regrid(").append(query);
		for(int i = 0; i < aggWindow.length; i++) {
			sb.append(",").append(aggWindow[i]);
		}
		while(summaryFunctions.hasNext()) {
			sb.append(",").append(summaryFunctions.next());
		}
		sb.append(")");
		return sb.toString();
	}
	
	// get the SciDB schema for this query
	protected String generateShowQuery(String query) {
		String escapedQuery = query.replace("'", "\\'");
		return "show('"+escapedQuery+"','afl')";
	}
	
	// get the SciDB schema for this array
	protected String generateShowQueryForArray(String arrayName) {
		return "show("+arrayName+")";
	}
	
	//store the result of this query at the given store name
	protected String generateStoreQuery(String storeName, String query) {
		return "store("+query+","+storeName+")";
	}
	
	// given an array name, performs a scan on the array
	protected String generateScanQueryForArray(String arrayName) {
		return "scan("+arrayName+")";
	}
	
	// get attribute description for the given array name:
	// <name,type_id,nullable>[No]
	//No = sequence id
	protected String generateAttributesQueryForArray(String arrayName) {
		return "attributes("+arrayName+")";
	}
	
	// get dimension descriptions for the given array name:
	// <name,start,length,chunk_interval,chunk_overlap,low,high,type>[No]
	//No = sequence id
	protected String generateDimensionsQueryForArray(String arrayName) {
		return "dimensions("+arrayName+")";
	}
	
	// given a scidb dimensions definition, gets the data types
	// TODO: make this function find datatypes, instead of assuming all dims are int64
	public Map<String,List<Integer>> parseSchemaDimensionsForBoundaries(String dimensions) {
		Map<String,List<Integer>> boundaryMap = new HashMap<String,List<Integer>>();
		int totalDimensions = dimensions.split("=").length - 1;
		String[] tokens = dimensions.split(",");
		int base = 0;
		for(int i = 0; i < totalDimensions; i++,base+=3) {
			String first = tokens[base];
			String[] name_tokens = first.split("=");
			String name = name_tokens[0];
			String[] boundary_tokens = name_tokens[1].split(":");
			int low = Integer.parseInt(boundary_tokens[0]);
			int high = Integer.parseInt(boundary_tokens[1]);
			//int chunk_width = Integer.parseInt(tokens[base+1]);
			//int chunk_overlap = Integer.parseInt(tokens[base+2]);
			List<Integer> temp = new ArrayList<Integer>();
			temp.add(low);
			temp.add(high);
			boundaryMap.put(name,temp);
		}
		return boundaryMap;
	}
	
	// given a scidb dimensions definition, gets the data types
	// TODO: make this function find datatypes, instead of assuming all dims are int64
	public List<String> parseSchemaDimensionsForDataTypes(String dimensions) {
		List<String> dataTypes = new ArrayList<String>();
		int totalDimensions = dimensions.split("=").length - 1;
		for(int i = 0; i < totalDimensions; i++) {
			dataTypes.add("int64");
		}
		return dataTypes;
	}
	
	// given a scidb attributes definition, gets the data types
	public List<String> parseSchemaAttributesForDataTypes(String attributes) {
		List<String> dataTypes = new ArrayList<String>();
		String[] toParse = attributes.split(",");
		for(int i = 0; i < toParse.length; i++) {
			String sub = toParse[i];
			String dataType = sub.split(":")[1].trim().split(" ")[0];
			dataTypes.add(dataType);
		}
		return dataTypes;
	}
	
	// parses a scidb schema for both the dimension and attribute data types
	public Map<String,List<Integer>> parseSchemaForDimensionBoundaries(String schema) {
		Map<String,List<Integer>> boundaryMap = null;
		int dimsStart = schema.indexOf("[");
		int dimsEnd = schema.indexOf("]");
		String dimensions = schema.substring(dimsStart+1,dimsEnd);
		boundaryMap = parseSchemaDimensionsForBoundaries(dimensions);
		return boundaryMap;
	}
	
	// parses a scidb schema for both the dimension and attribute data types
	public List<String> parseSchemaForDataTypes(String schema) {
		List<String> dataTypes = new ArrayList<String>();
		int attributesStart = schema.indexOf("<");
		int attributesEnd = schema.indexOf(">");
		int dimsStart = schema.indexOf("[");
		int dimsEnd = schema.indexOf("]");
		String attributes = schema.substring(attributesStart+1,attributesEnd);
		String dimensions = schema.substring(dimsStart+1,dimsEnd);
		dataTypes.addAll(parseSchemaDimensionsForDataTypes(dimensions));
		dataTypes.addAll(parseSchemaAttributesForDataTypes(attributes));
		return dataTypes;
	}
}
