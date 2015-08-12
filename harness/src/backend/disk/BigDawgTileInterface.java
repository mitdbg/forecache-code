package backend.disk;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import configurations.BigDawgConfig;
import configurations.Config;
import configurations.VMConfig;

import utils.DBInterface;
import utils.UtilityFunctions;
import backend.util.NiceTile;
import backend.util.Params;
import backend.util.TileKey;

public class BigDawgTileInterface {
	public static int simulation_build_delay = 33679; // in ms, derived empirically
	public DiskNiceTileBuffer simulation_buffer;
	public static String url = "localhost";
	public static int port = 8080;
	private Map<String,Boolean> isBuilt;
	
	public BigDawgTileInterface() {
		this.isBuilt = new HashMap<String,Boolean>();
	}
	
	public String buildArrayName(String recordName, int aggWindow) {
		return "FC_wf"+recordName+"_window"+aggWindow;
	}
	
	// shortcut function to check if a zoom level is available,
	// and to build it if it's not built yet
	public synchronized boolean checkAndBuild(String recordName, int aggWindow) throws Exception {
		if(!checkForArray(recordName, aggWindow)) return buildZoomLevel(recordName,aggWindow);
		return true;
	}
	
	// does the array exist in SciDB?
	public synchronized boolean checkForArray(String recordName,int aggWindow) throws Exception {
		String arrayName = buildArrayName(recordName,aggWindow);
		if(this.isBuilt.containsKey(arrayName)) {
			return this.isBuilt.get(arrayName);
		}
		String query = "ARRAY(show("+arrayName+"))";
		String result = sendRequest(query);
		BigDawgResponseObject obj = null;
		try {
			obj  = parseBDJsonObject(result);
			this.isBuilt.put(arrayName, obj.tuples.length > 0);
		} catch(Exception e) {
			System.out.println("could not parse JSON object");
			e.printStackTrace();
		}
		return (obj != null) && (obj.tuples.length > 0);
	}
	
	// did we successfully build the zoom level?
	public synchronized boolean buildZoomLevel(String recordName, int aggWindow) throws Exception {
		String arrayName = buildArrayName(recordName,aggWindow);
		String query = "ARRAY(subarray(store(regrid(apply(filter(slice(waveform_signal_table,RecordName,"+recordName+"),not(is_nan(signal))),msec,msec),"+aggWindow+",avg(signal) as avg_signal),"+arrayName+"),0,1))";
		BigDawgResponseObject obj  = parseBDJsonObject(sendRequest(query));
		return obj.tuples.length > 0;
	}
	
	// get the raw big dawg output for this particular tile
	public synchronized String getRawTile(int tileID, int k, String recordName, int aggWindow) throws Exception {
		if(checkAndBuild(recordName,aggWindow)) {
			int low = tileID*k;
			int high = tileID*k+k-1;
			String query = "ARRAY(subarray(apply(FC_wf"+recordName+"_window"+aggWindow+",msec2,msec), "+low+","+high+"))";
			return sendRequest(query);
		} else {
			throw new Exception("Could not build zoom level!");
		}
	}
	
	// should be able to successfully send queries to BigDAWG
	public synchronized String sendRequest(String query) throws Exception {
		String urlstring = "http://"+url+":"+port+"/bigdawg/query";
		String urlparams = "{\"query\":\""+query+"\"}";
		byte[] postData = urlparams.getBytes(StandardCharsets.UTF_8);
		System.out.println("url:"+urlstring);
		System.out.println("params:"+urlparams);
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
	
	public static void main(String[] args) throws Exception {
		Config conf = new VMConfig();//new BigDawgConfig();
		conf.setConfig();
		String query = "ARRAY(subarray(list('arrays'),0,0))";
		BigDawgTileInterface bdInterface = new BigDawgTileInterface();
		String response = bdInterface.sendRequest(query);
		System.out.println("result:'"+response+"'");
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		BigDawgResponseObject obj = mapper.readValue(response, BigDawgResponseObject.class);
		System.out.println(obj.tuples[0][1]);
	}
}
