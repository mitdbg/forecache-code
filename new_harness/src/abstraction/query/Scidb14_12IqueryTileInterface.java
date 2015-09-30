package abstraction.query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import abstraction.tile.ColumnBasedNiceTile;
import abstraction.util.UtilityFunctions;

import abstraction.util.TileStructure;
import abstraction.util.View;

import configurations.DBConnector;

public class Scidb14_12IqueryTileInterface extends Scidb14_12TileInterface {
	protected String scidbVersion = "14.12";
	protected String outputFormat = "tsv+";
			
	public Scidb14_12IqueryTileInterface() {
		super();
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
	
	// just gets the data as a gigantic string.
	// probably won't be used with scidb connectors
	public String getRawData(String query) {
		List<String> rawData = new  ArrayList<String>();
		String[] cmd = buildCmd(query);
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
			//long start = System.currentTimeMillis();
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			for (String line; (line = br.readLine()) != null;) {
				rawData.add(line);
			}
			return UtilityFunctions.consolidateStrings(rawData);
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
		return null;
	}

	/************* For Precomputation ***************/

	/************* Helper Functions ***************/
	
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
			if(pair.attributes.size() == 0) {
				if(retrieve_output) {
					returnval = false;
				} else {
					List<String> errorLines = new ArrayList<String>();
					BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
					for (String line; (line = ebr.readLine()) != null;) {
						errorLines.add(line);
					}
					if(errorLines.size() > 0) {
						returnval = false;
						System.err.println(UtilityFunctions.consolidateStrings(errorLines));
					} else {
						returnval = true;
					}
				}
			}
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
		
		// prints the entire dataset
		for(int j = 0; j < tile.getSize(); j++) {
			for(int i = 0; i < tile.attributes.size(); i++) {
				System.out.println(tile.get(i, 0));
			}
			System.out.println();
		}
	}
	
	
	protected static class AttributesDataPair {
		public List<String> attributes = null;
		public List<String> data = null;
	}
}
