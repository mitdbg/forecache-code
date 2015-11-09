package abstraction.query;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


import abstraction.enums.DBConnector;
import abstraction.query.Scidb14_12IqueryTileInterface.AttributesDataPair;
import abstraction.structures.NewTileKey;
import abstraction.structures.TileStructure;
import abstraction.structures.View;
import abstraction.tile.ColumnBasedNiceTile;

public class BigDawgScidbTileInterface extends Scidb14_12TileInterface {
	protected String host = "localhost";
	protected int port = 8080;
	
	public BigDawgScidbTileInterface() {
		this.connectionType = DBConnector.BIGDAWG;
	}
	
	public BigDawgScidbTileInterface(int port, String host) {
		this();
		this.host = host;
		this.port = port;
	}

	@Override
	public synchronized boolean executeQuery(String query) {
		AttributesDataPair pair = new AttributesDataPair();
		return getRawDataHelper(query, pair, true);
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
		query = addBigDawgWrapper(query);
		return getRawTileHelper(query,tile,true);
	}

	@Override
	public String getRawData(String query) {
		try {
			query = addBigDawgWrapper(query);
			return sendRequest(query);
		} catch(Exception e) {
			System.err.println("error occured while sending request to big dawg");
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<String> getQueryDataTypes(String query) {
		query = removeBigDawgWrapper(query);
		String showQuery = generateShowQuery(query);
		String bdq = addBigDawgWrapper(showQuery);
		ColumnBasedNiceTile t = new ColumnBasedNiceTile();
		getRawTile(bdq,t);
		int schema_index = t.getIndex("schema");
		String schema = (String) t.get(schema_index, 0);
		List<String> dataTypes = parseSchemaForDataTypes(schema);
		return dataTypes;
	}
	
	@Override
	public DimensionBoundary getDimensionBoundaries(String query) {
		query = removeBigDawgWrapper(query);
		String showQuery = generateShowQuery(query);
		String bdq = addBigDawgWrapper(showQuery);
		ColumnBasedNiceTile t = new ColumnBasedNiceTile();
		getRawTile(bdq,t);
		int schema_index = t.getIndex("schema");
		String schema = (String) t.get(schema_index, 0);
		return parseSchemaForDimensionBoundaries(schema);
	}
	
	/************* For Precomputation ***************/
	
	//is this zoom level already built?
	@Override
	public boolean checkZoomLevel(View v, TileStructure ts, int zoomLevel) {
		String query = getZoomLevelName(v,ts,zoomLevel);
		String showQuery = generateShowQueryForArray(query);
		String bdq = addBigDawgWrapper(showQuery);
		return executeQuery(bdq);
	}
	
	@Override
	public boolean buildZoomLevel(View v, TileStructure ts, int zoomLevel) {
		String query = generateBuildZoomLevelQuery(v,ts,zoomLevel);
		String storeQuery = generateStoreQuery(getZoomLevelName(v,ts,zoomLevel),query);
		String bdsq = addBigDawgWrapper(storeQuery); // wrap in big dawg syntax
		return executeQuery(bdsq);
	}
	
	@Override
	public boolean checkTile(View v, TileStructure ts, NewTileKey id) {
		String query = getTileName(v,ts,id);
		String showQuery = generateShowQueryForArray(query);
		String bdsq = addBigDawgWrapper(showQuery); // wrap in big dawg syntax
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
		String tileName = getTileName(v,ts,id);
		String query = this.generateScanQueryForArray(tileName);
		String bdq = addBigDawgWrapper(query);
		return getTile(bdq,tile);
	}
	
	@Override
	public boolean retrieveTileFromStoredZoomLevel(View v,
			TileStructure ts, ColumnBasedNiceTile tile, NewTileKey id) {
		String query = generateRetrieveTileQuery(v,ts,id);
		String bdq = addBigDawgWrapper(query);
		return getTile(bdq,tile);
	}
	
	/************* Helper Functions ***************/
	
	// wrap SciDB queries in "ARRAY(?)" syntax
	public String addBigDawgWrapper(String query) {
		String withoutWrapper = removeBigDawgWrapper(query);
		return "ARRAY("+withoutWrapper+")";
	}
	
	// if query has big dawg wrapper syntax,
	// remove this syntax
	public String removeBigDawgWrapper(String query) {
		if(query.startsWith("ARRAY(") && query.endsWith(")")) {
			return query.substring(6,query.length()-1);
		}
		return query;
	}
	
	// get the SciDB schema for this query
	@Override
	protected String generateShowQuery(String query) {
		String escapedQuery = query.replace("'", "\\\\'");
		return "show('"+escapedQuery+"','afl')";
	}
	
	// have to override this function, to make sure view query gets stripped
	// of big dawg wrapper syntax
	@Override
	protected String generateBuildZoomLevelQuery(View v, TileStructure ts, int zoom) {
		String query = removeBigDawgWrapper(v.getQuery());
		int[] aggWindow = ts.aggregationWindows[zoom];
		Iterator<String> summaryFunctions = v.getSummaryFunctions();
		return generateRegridQuery(query,aggWindow,summaryFunctions);
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
	
	public synchronized boolean getRawDataHelper(String query, AttributesDataPair pair, boolean retrieve_output) {
		try {
			String result = sendRequest(query);
			if(retrieve_output) {
				BigDawgResponseObject bdro = parseBDJsonObject(result);
				return getDataFromBigDawgResponseObject(bdro,pair);
			}
			return true;
		} catch(Exception e) {
			System.err.println("error occured while sending request to big dawg");
			e.printStackTrace();
		}
		return false;
	}
	
	protected boolean getDataFromBigDawgResponseObject(BigDawgResponseObject bdro,
			AttributesDataPair pair) {
		pair.attributes = new ArrayList<String>();
		pair.data = new ArrayList<String>();
		try {
			for(int i = 0; i < bdro.schema.length; i++) {
				pair.attributes.add(bdro.schema[i]);
			}
			for(int j = 0; j < bdro.tuples.length; j++) { // for each row
				for(int i = 0; i < bdro.schema.length; i++) { // for each attribute
					pair.data.add(bdro.tuples[j][i]);
				}
			}
			return true;
		} catch(Exception e) {
			System.err.println("could not extract data from big dawg response object");
			e.printStackTrace();
		}
		return false;
	}
	
	// make big dawg http request, then get the raw result string
	protected synchronized String sendRequest(String query) throws Exception {
		String urlstring = "http://"+host+":"+port+"/bigdawg/query";
		String urlparams = "{\"query\":\""+query+"\"}";
		byte[] postData = urlparams.getBytes(StandardCharsets.UTF_8);
		//System.out.println("url:"+urlstring);
		//System.out.println("params:"+urlparams);
		URL geturl = null;
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		StringBuffer sbuffer = new StringBuffer();
		String line;
		String result = null;
		long diff = 0;
		try {
			geturl = new URL(urlstring);
		} catch (MalformedURLException e) {
			System.out.println("error occurred while retrieving url object for: '"+urlstring+"'");
			e.printStackTrace();
		}
		if(geturl == null) {
			return result;
		}

		try {
			connection = (HttpURLConnection) geturl.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			//connection.setRequestProperty("Accept","*/*");
			connection.setRequestProperty( "Content-Type", "application/json; "+"charset=UTF-8" );
			connection.setRequestProperty( "Content-Length", Integer.toString( postData.length ));
			connection.setUseCaches( false );
			
			// send params
			DataOutputStream wr = new DataOutputStream( connection.getOutputStream());
			wr.write( postData );
			
			diff = System.currentTimeMillis();
			
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while((line = reader.readLine()) != null) {
				sbuffer.append(line);
			}
			reader.close();

			diff = System.currentTimeMillis() - diff;
			result = sbuffer.toString();
			System.out.println(result);
			if(result.equals("error")) {
				throw new Exception("serious error occurred on backend while retrieving query result");
			}
		} catch (IOException e) {
			System.out.println("Error retrieving response from url: '"+urlstring+"'");
			e.printStackTrace();
			/*
			// only uncomment this to print the error response
			reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
			while((line = reader.readLine()) != null) {
				sbuffer.append(line);
			}
			reader.close();
			result = sbuffer.toString();
			*/
		}

		if(connection != null) {
			connection.disconnect();
		}
		if(reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/*
	 * Need to be able to parse the raw big dawg output, so I can do calculations and make predictions!
	 */
	public BigDawgResponseObject parseBDJsonObject(String jsonstring) {
		BigDawgResponseObject obj = null;
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		try {
			obj= mapper.readValue(jsonstring, BigDawgResponseObject.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			System.err.println("could not parse json object...");
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return obj;
	}
	
	// used for deserialization of the JSON data from big dawg
	public static class BigDawgResponseObject {
		public String message;
		public Integer responseCode;
		public Integer pageNumber;
		public Integer totalPages;
		public String[] schema; // has the name of all dimensions/attributes, in order
		public String[] types; // has datatypes from postgres (not currently supported in SciDB)
		public String[][] tuples; // has an array of arrays, each array is a tuple
		public Long cacheTimestamp;
	}
	
	public static void main(String[] args) {
		//String query = "ARRAY(list('arrays'))";
		//String query = "ARRAY(subarray(slice(waveform_signal_table,RecordName,325553800011),100,110))";
		String query = "ARRAY(subarray(apply(filter(slice(waveform_signal_table,RecordName,325553800011),not(is_nan(signal))),msec2,msec),0,10))";
		BigDawgScidbTileInterface bdi = new BigDawgScidbTileInterface();
		ColumnBasedNiceTile tile = new ColumnBasedNiceTile();
		List<String> dataTypes = bdi.getQueryDataTypes(query);
		for(String dt : dataTypes) {
			System.out.println(dt);
		}
		System.out.println();
		bdi.getTile(query, tile);
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
		
		// prints the entire dataset
		for(int j = 0; j < tile.getSize(); j++) {
			for(int i = 0; i < tile.attributes.size(); i++) {
				System.out.println(tile.get(i, 0));
			}
			System.out.println();
		}
		
		// making sure executeQuery returns appropriate boolean values
		// i.e., bad queries should result in a false from executeQuery
		System.out.println("query successful? "+bdi.executeQuery("ARRAY(show(wwwwww))"));
	}
}
