package abstraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import backend.util.NiceTile;
import backend.util.Params;

import configurations.DBConnector;

public class Scidb14_12IqueryTileInterface extends NewTileInterface {
	protected String scidbVersion = "14.12";
	protected String outputFormat = "tsv+";
			
	public Scidb14_12IqueryTileInterface() {
		super(DBConnector.SCIDB);
	}

	@Override
	public TileStructure buildDefaultTileStructure(View v) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public synchronized boolean executeQuery(String query) {
		// don't waste time making a tile
		AttributesDataPair pair = new AttributesDataPair();
		boolean returnval = getRawDataHelper(query,pair,false);
		return returnval;
	}

	// just a generic function to execute a query on the DBMS, and retrieve the output
	@Override
	public boolean getTile(String query, ColumnBasedNiceTile tile) {
		// TODO Auto-generated method stub
		if(tile.dataTypes == null) { // get the data types
			try{
					tile.dataTypes = new ArrayList<Class<?>>();
					// issues query to get data types
					List<String> rawTypes = getQueryDataTypes(query);
					for(String rt : rawTypes) {
						tile.dataTypes.add(getColumnTypeInJava(rt));
					}
			} catch(Exception e) {
				System.err.println("error occured while retrieveing data types in scidb using iquery for query '"+query+"'...");
				e.printStackTrace();
				return false;
			}
		}
		return getTileHelper(query,tile);
	}

	// just a generic function to execute a query on the DBMS, and retrieve the output
	// assumes data types are all strings
	@Override
	public boolean getRawTile(String query, ColumnBasedNiceTile tile) {
		return getRawTileHelper(query,tile,true);
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

	@Override
	public boolean buildZoomLevel(View v, TileStructure ts, int zoomLevel) {
		String query = generateRetrieveZoomLevelQuery(v,ts,zoomLevel);
		String storeQuery = generateStoreQuery(getZoomLevelName(v,ts,zoomLevel),query);
		return executeQuery(storeQuery);
	}

	@Override
	public boolean buildTile(View v, TileStructure ts, ColumnBasedNiceTile tile, NewTileKey id) {
		String query = generateRetrieveTileQuery(v,ts,id);
		String storeQuery = generateStoreQuery(getTileName(v,ts,id),query);
		return getTile(storeQuery,tile);
	}

	@Override
	public boolean retrieveStoredTile(View v, TileStructure ts, ColumnBasedNiceTile tile, NewTileKey id) {
		String query = generateRetrieveTileQuery(v,ts,id);
		return getTile(query,tile);
	}


	/************* Helper Functions ***************/


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

	protected String generateRetrieveZoomLevelQuery(View v, TileStructure ts, int zoom) {
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
	
	// executes tile and retrieves output as a tile
	// assumes data types have been specified in tile
	public synchronized boolean getTileHelper(String query,ColumnBasedNiceTile tile) {
		AttributesDataPair pair = new AttributesDataPair();
		boolean returnval = getRawDataHelper(query,pair,true);
		 // assumes data types have already been specified
		tile.initializeData(pair.data, pair.attributes);
		return returnval;
	}
	
	// executes query and optionally retrieves the output
	public synchronized boolean getRawTileHelper(String query,ColumnBasedNiceTile tile, boolean retrieve_output) {
		AttributesDataPair pair = new AttributesDataPair();
		boolean returnval = getRawDataHelper(query,pair,retrieve_output);
		tile.initializeDataDefault(pair.data, pair.attributes);
		return returnval;
	}
	
	public synchronized String[] buildCmdNoOutput(String query) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER="+scidbVersion+" ; "+
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; "+
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; "+
				"source ~/.bashrc ; iquery -o "+outputFormat+" -aq \"" + query + "\"";
		return myresult;
	}
	
	public synchronized String[] buildCmd(String query) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER="+scidbVersion+" ; "+
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; "+
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; "+
				"source ~/.bashrc ; iquery -o "+outputFormat+" -aq \"" + query + "\"";
		return myresult;
	}

	// gets the raw data, and the attribute/dimension names
	public synchronized boolean getRawDataHelper(String query,AttributesDataPair pair, boolean retrieve_output) {
		boolean returnval = false;
		String[] cmd;
		if(retrieve_output) {
			cmd = buildCmd(query);
		} else {
			cmd = buildCmdNoOutput(query);
		}

		// print command
		//UtilityFunctions.printStringArray(cmd);
		//System.out.println();

		Process proc;
		try {
			proc = Runtime.getRuntime().exec(cmd);
			/*
		// only uncomment this if things aren't working
		BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		for (String line; (line = ebr.readLine()) != null;) {
			System.out.println(line);
		}
			 */
			List<String> temp = new ArrayList<String>();
			List<String> labels = new ArrayList<String>();
			//long start = System.currentTimeMillis();
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			boolean first = true;
			for (String line; (line = br.readLine()) != null;) {
				//System.out.println(line);
				String[] tokens;// = line.split(",");
				if(!first) { // ignore first line
					tokens = line.split(",",labels.size());
					for(int i = 0; i < tokens.length; i++) {
						temp.add(tokens[i]); // just add 0.0 if we can't parse it
					}
				} else {
					first = false;
					tokens = line.split(",");
					for(int i = 0; i < tokens.length;i++) {
						labels.add(tokens[i]);
					}
				}
			}
			pair.attributes = labels;
			pair.data = temp;
			returnval = true;
		} catch (IOException e) {
			System.out.println("Error occurred while reading query output.");
			e.printStackTrace();
		}
		/*
		for(int i = 0; i < tile.attributes.size(); i++) {
			System.out.print(tile.attributes.get(i)+"\t");
			System.out.println(tile.data.get(i).size());
		}
		 */
		//long end = System.currentTimeMillis();
		//System.out.println("time to build: "+(end - start) +"ms");
		return returnval;
	}
	
	public static void main(String[] args) {
		String query = "list('arrays')";
		ColumnBasedNiceTile tile = new ColumnBasedNiceTile();
		Scidb14_12IqueryTileInterface sdbi = new Scidb14_12IqueryTileInterface();
		List<String> dataTypes = sdbi.getQueryDataTypes(query);
		for(String dt : dataTypes) {
			System.out.println(dt);
		}
		System.out.println();
		sdbi.getTile(query, tile);
		for(String a : tile.attributes) {
			System.out.println(a);
		}
		System.out.println();
		System.out.println(tile.getSize());
		System.out.println();
		for(Class<?> c : tile.dataTypes) {
			System.out.println(c);
		}
		System.out.println();
		System.out.println(tile.get(0, 0));
		System.out.println(tile.get(1, 0));
		System.out.println(tile.get(2, 0));
		System.out.println(tile.get(3, 0));
		System.out.println(tile.get(4, 0));
	}
	
	
	protected static class AttributesDataPair {
		public List<String> attributes = null;
		public List<String> data = null;
	}
}
