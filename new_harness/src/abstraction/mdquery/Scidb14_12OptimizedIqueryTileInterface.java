package abstraction.mdquery;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;


import abstraction.enums.DBConnector;
import abstraction.query.NewTileInterface.DimensionBoundary;
import abstraction.mdstructures.MultiDimTileKey;
import abstraction.structures.SignatureMap;
import abstraction.mdstructures.MultiDimTileStructure;
import abstraction.structures.View;
import abstraction.tile.MultiDimColumnBasedNiceTile;
import abstraction.util.UtilityFunctions;



public class Scidb14_12OptimizedIqueryTileInterface extends Scidb14_12TileInterface {
	protected String scidbVersion = "14.12";
	protected String outputFormat = "tsv+";
	protected String delim = "\t";
	
	protected Map<Integer,Map<Integer,Boolean>> zoomLevelMap;
	protected Map<Integer,Map<MultiDimTileKey,Boolean>> tileMap;
	protected Map<String,DimensionBoundary> dimbounds; // does not depend on tile structures
	
			
	public Scidb14_12OptimizedIqueryTileInterface() {
		super();
		
		//initialize maps
		zoomLevelMap = new HashMap<Integer,Map<Integer,Boolean>>();
		tileMap = new HashMap<Integer,Map<MultiDimTileKey,Boolean>>();
		dimbounds = new HashMap<String,DimensionBoundary>();
	}
	
	@Override
	public boolean getTile(View v, MultiDimTileStructure ts, MultiDimColumnBasedNiceTile tile) {
		boolean built = true;
		if(!checkTile(v,ts,tile.id)) { // tile is not prepared
			System.out.println("building tile first...");
			built = this.buildTile(v, ts, tile, tile.id);
		}
		/*
		if(!checkZoomLevel(v,ts,tile.id.zoom)) { // zoom level is not prepared
			built = buildZoomLevel(v,ts,tile.id.zoom);
		}
		*/
		System.out.println("fetching tile...");
		return built && this.retrieveStoredTile(v, ts, tile, tile.id);
		//return built && this.retrieveTileFromStoredZoomLevel(v, ts, tile, tile.id);
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
	public boolean getTile(String query, MultiDimColumnBasedNiceTile tile) {
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
	public boolean getRawTile(String query, MultiDimColumnBasedNiceTile tile) {
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
	
	@Override
	public boolean checkZoomLevel(View v, MultiDimTileStructure ts, int[] zoomPos) {
		int hash = hashVTS(v,ts);
		checkVTS(v,ts);
		HashCodeBuilder hb = new HashCodeBuilder();
		for(int i = 0; i < zoomPos.length; i++) {
			hb.append(zoomPos[i]);
		}
		int code = hb.toHashCode();
		Map<Integer,Boolean> m = this.zoomLevelMap.get(code);
		return m.containsKey(code);
	}
	
	// get rid of all the tiles we made for this view
	public boolean removeAllTiles(View v, MultiDimTileStructure ts) {
		int hash = hashVTS(v,ts);
		checkVTS(v,ts);
		DimensionBoundary dimbound = getDimensionBoundary(v,ts);
		boolean returnval = true;
		int total = 1;
		for(int i = 0; i < ts.dimensionGroups.length;i++) {
			total *= ts.dimensionGroups[i].length;
		}
		int[] zoomPos = new int[ts.dimensionGroups.length];
		for(int i = 0; i < total; i++) {
			Map<MultiDimTileKey,Boolean> allTiles = getAllTileKeysForZoomLevel(ts,dimbound,Arrays.copyOf(zoomPos, zoomPos.length));
			List<MultiDimTileKey> tileKeys = new ArrayList<MultiDimTileKey>(allTiles.keySet());
			for(MultiDimTileKey key : tileKeys) {
				returnval &= removeTile(v,ts,key);
			}
			
			// get the next zoom position
			zoomPos[0]++;
			for(int j = 0; j < (zoomPos.length-1); j++) {
				if(zoomPos[j] == ts.dimensionGroups[j].length) {
					zoomPos[j]--;
					zoomPos[j+1]++;
				}
			}
		}
		this.zoomLevelMap.put(hash, new HashMap<Integer,Boolean>());
		this.saveZoomMap(v, ts);
		return returnval;
	}
	
	public boolean removeTile(View v, MultiDimTileStructure ts, MultiDimTileKey id) {
		int hash = hashVTS(v,ts);
		checkVTS(v,ts);
		Map<MultiDimTileKey,Boolean> m = this.tileMap.get(hash);
		String removeQuery = generateRemoveQuery(getTileName(v,ts,id));
		boolean returnval = getRawTileHelper(removeQuery,null,false);
		if(returnval) {
			m.remove(id); // track the change
		}
		this.tileMap.put(hash, m);
		this.saveTileMap(v, ts);
		return returnval;
	}
	
	// build entire zoom level
	@Override
	public boolean buildZoomLevel(View v, MultiDimTileStructure ts, int[] zoomPos) {
		int hash = hashVTS(v,ts);
		checkVTS(v,ts);
		Map<MultiDimTileKey,Boolean> m = this.tileMap.get(hash);
		DimensionBoundary dimbound = getDimensionBoundary(v,ts);
		Map<MultiDimTileKey,Boolean> allTiles = getAllTileKeysForZoomLevel(ts,dimbound,zoomPos);
		List<MultiDimTileKey> tileKeys = new ArrayList<MultiDimTileKey>(allTiles.keySet());
		boolean returnval = true;
		MultiDimColumnBasedNiceTile t = new MultiDimColumnBasedNiceTile();
		for(int i = 0; i < tileKeys.size(); i++) {
			MultiDimTileKey key = tileKeys.get(i);
			String buildQuery = generateBuildTileQuery(v, ts, key);
			String storeQuery = generateStoreQuery(getTileName(v,ts,key),buildQuery);
			//System.out.println("store query: "+storeQuery);
			//System.out.println("building tile '"+key+"' for view '"+v.getName()+"'");
			boolean test = getRawTileHelper(storeQuery,t,false);
			if(test) {
				m.put(key, true); // track the tiles we've built
			}
			returnval &= test;
		}
		if(returnval) { // successfully built zoom level
			Map<Integer,Boolean> z = this.zoomLevelMap.get(hash);
			HashCodeBuilder hb = new HashCodeBuilder();
			for(int i = 0; i < zoomPos.length; i++) {
				hb.append(zoomPos[i]);
			}
			int code = hb.toHashCode();
			z.put(code, true);
			this.zoomLevelMap.put(hash, z);
			this.saveZoomMap(v, ts);
		}
		this.tileMap.put(hash, m);
		this.saveTileMap(v, ts);
		/*
		String query = generateBuildZoomLevelQuery(v,ts,zoomLevel);
		String storeQuery = generateStoreQuery(getZoomLevelName(v,ts,zoomLevel),query);
		boolean result = executeQuery(storeQuery);
		if(result) {
			int hash = hashVTS(v,ts);
			Map<Integer,Boolean> m = this.zoomLevelMap.get(hash);
			this.zoomLevelMap.put(hash, m);
			this.saveZoomMap(v, ts);
		}
		*/
		return returnval;
	}
	
	@Override
	public boolean checkTile(View v, MultiDimTileStructure ts, MultiDimTileKey id) {
		int hash = hashVTS(v,ts);
		checkVTS(v,ts);
		Map<MultiDimTileKey,Boolean> m = this.tileMap.get(hash);
		return m.containsKey(id);
	}

	// do not assume you can fetch the tile as you build it. SciDB will not support this
	// functionality in the future
	@Override
	public boolean buildTile(View v, MultiDimTileStructure ts, MultiDimColumnBasedNiceTile tile, MultiDimTileKey id) {
		String query = generateBuildTileQuery(v,ts,id);
		String storeQuery = generateStoreQuery(getTileName(v,ts,id),query);
		//boolean returnval = getTile(storeQuery,tile);
		boolean returnval = this.getRawTileHelper(storeQuery, tile, false);
		if(returnval) { // successfully built the tile
			int hash = hashVTS(v,ts);
			checkVTS(v,ts);
			Map<MultiDimTileKey,Boolean> m = this.tileMap.get(hash);
			m.put(id, true); // the tile has been built
			this.tileMap.put(hash, m);
			this.saveTileMap(v, ts);
		}
		return returnval;
	}

	@Override
	public boolean retrieveStoredTile(View v, MultiDimTileStructure ts, MultiDimColumnBasedNiceTile tile, MultiDimTileKey id) {
		String tileName = getTileName(v,ts,id);
		String query = this.generateScanQueryForArray(tileName);
		return getTile(query,tile);
	}
	
	@Override
	public boolean retrieveTileFromStoredZoomLevel(View v,
			MultiDimTileStructure ts, MultiDimColumnBasedNiceTile tile, MultiDimTileKey id) {
		String query = generateRetrieveTileQuery(v,ts,id);
		return getTile(query,tile);
	}
	
	/************* Helper Functions ***************/
	
	// do we have data for this particular view and tile structure?
	protected void checkVTS(View v, MultiDimTileStructure ts) {
		int hash = hashVTS(v,ts);
		
		if(!this.zoomLevelMap.containsKey(hash)) {
			if(!this.loadZoomMap(v, ts)) {
				Map<Integer,Boolean> temp = new HashMap<Integer,Boolean>();
				this.zoomLevelMap.put(hash, temp);
				this.saveZoomMap(v, ts); // create a new file for this map
			}
		}
		
		if(!this.tileMap.containsKey(hash)) {
			if(!this.loadTileMap(v, ts)) {
				Map<MultiDimTileKey,Boolean> temp = new HashMap<MultiDimTileKey,Boolean>();
				this.tileMap.put(hash, temp);
				this.saveTileMap(v,ts); // create a new file for this map
			}
		}
	}
	
	protected boolean loadZoomMap(View v, MultiDimTileStructure ts) {
		StringBuilder sb = new StringBuilder();
		int hash = hashVTS(v,ts);
		sb.append("zoomMap_");
		sb.append(hash);
		sb.append(".ser");
		String filename = sb.toString();
		
		Map<Integer,Boolean> m = null;
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
	        m = (HashMap<Integer,Boolean>) in.readObject();
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+filename);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+filename);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+HashMap.class);
			e.printStackTrace();
		}
		if(m == null) return false;
		this.zoomLevelMap.put(hash, m);
		return true;
	}
	
	protected boolean loadTileMap(View v, MultiDimTileStructure ts) {
		StringBuilder sb = new StringBuilder();
		int hash = hashVTS(v,ts);
		sb.append("tileMap_");
		sb.append(hash);
		sb.append(".ser");
		String filename = sb.toString();
		
		Map<MultiDimTileKey,Boolean> m = null;
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
	        m = (HashMap<MultiDimTileKey,Boolean>) in.readObject();
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+filename);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+filename);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+HashMap.class);
			e.printStackTrace();
		}
		if(m == null) return false;
		this.tileMap.put(hash, m);
		return true;
	}
	
	protected void saveZoomMap(View v, MultiDimTileStructure ts) {
		int hash = hashVTS(v,ts);
		Map<Integer,Boolean> m = this.zoomLevelMap.get(hash);
		if(m == null) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("zoomMap_");
		sb.append(hash);
		sb.append(".ser");
		File file = new File(sb.toString());
		
		ObjectOutputStream out;
		try {
			// do not append
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file, false)));
			out.writeObject(m);
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not write zoom map "+sb.toString()+" to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write zoom map "+sb.toString()+" to disk.");
			e.printStackTrace();
		}
	}
	
	// will save the corresponding tile map for this particular view and tile structure
	protected void saveTileMap(View v, MultiDimTileStructure ts) {
		int hash = hashVTS(v,ts);
		Map<MultiDimTileKey,Boolean> m = this.tileMap.get(hash);
		if(m == null) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("tileMap_");
		sb.append(hash);
		sb.append(".ser");
		File file = new File(sb.toString());
		
		ObjectOutputStream out;
		try {
			// do not append
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file, false)));
			out.writeObject(m);
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not write tile map "+sb.toString()+" to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write tile map "+sb.toString()+" to disk.");
			e.printStackTrace();
		}
	}
	
	// get a hashcode for this pair
	protected int hashVTS(View v, MultiDimTileStructure ts) {
		HashCodeBuilder hcb = new HashCodeBuilder();
		hcb.append(v.getName());
		for(int i = 0; i < ts.aggregationWindows.length; i++) {
			for(int j = 0; j < ts.aggregationWindows[i].length; j++) {
				hcb.append(ts.aggregationWindows[i][j]);
			}
		}
		for(int j = 0; j < ts.tileWidths.length; j++) {
			hcb.append(ts.tileWidths[j]);
		}
		return hcb.toHashCode();
	}
	
	protected DimensionBoundary getDimensionBoundary(View v, MultiDimTileStructure ts) {
		String viewName = v.getName();
		DimensionBoundary dimbound = this.dimbounds.get(viewName);
		if(dimbound == null) {
			dimbound = this.getDimensionBoundaries(v.getQuery());
			if(dimbound != null) {
				this.dimbounds.put(viewName, dimbound);
			}
		}
		return dimbound;
	}
	
	// does not assume that zoom level is built
	@Override
	protected String generateBuildTileQuery(View v, MultiDimTileStructure ts,MultiDimTileKey id) {
		DimensionBoundary dimbound = getDimensionBoundary(v,ts);
		long[] highs = new long[ts.tileWidths.length];
		long[] lows = new long[ts.tileWidths.length];
		
		// get the base-0 range in aggregated points
		for(int i = 0; i < ts.tileWidths.length; i++) {
			lows[i] = ts.tileWidths[i]*id.dimIndices[i]; // in tiles
			highs[i] = lows[i] + ts.tileWidths[i]; // do not subtract 1 yet
		}
		
		// get the base-0 range in un-aggregated points
		long[] aggregationWindows = ts.getAggregationWindow(id.zoom);
		for(int i = 0; i < ts.tileWidths.length; i++) {
			lows[i] = lows[i] * aggregationWindows[i]; // in data points
			highs[i] = highs[i] * aggregationWindows[i]-1;
		}
		
		// shift/adjust the raw range to match the array
		for(int i = 0; i < ts.tileWidths.length; i++) {
			long min = dimbound.boundaryMap.get(dimbound.dimensions.get(i)).get(0);
			long max = dimbound.boundaryMap.get(dimbound.dimensions.get(i)).get(1);
			lows[i] = lows[i] + min; // map to dim ranges
			highs[i] = highs[i] + min;
			if(lows[i] < min) {
				lows[i] = min;
			}
			if(highs[i] < min) {
				highs[i] = min;
			}
			if(lows[i] > max) {
				lows[i] = max;
			}
			if(highs[i] > max) {
				highs[i] = max;
			}
		}
		// get the original query
		String sourceQuery = v.getQuery();
		// call subarray first, to ensure we only aggregate the relevant data
		String subarrayQuery = generateSubarrayQuery(sourceQuery,lows,highs);
		//String subarrayQuery = generateBetweenQuery(sourceQuery,lows,highs);
		// then do regrid to get aggregated data
		//String regridQuery = generateRegridQuery(subarrayQuery, 
		//		ts.aggregationWindows[id.zoom], v.getSummaryFunctions());
		String regridQuery = generateOptimizedRegridQuery(subarrayQuery, 
				ts.getAggregationWindow(id.zoom), v.getAttributeNames(), v.getSummaryFunctions(),
				v.getSummaryNames());
		//System.out.println("tile build query: "+regridQuery);
		return regridQuery;
	}
	
	// executes tile and retrieves output as a tile
	// assumes data types have been specified in tile
	public synchronized boolean getTileHelper(String query,MultiDimColumnBasedNiceTile tile) {
		AttributesDataPair pair = new AttributesDataPair();
		boolean returnval = getRawDataHelper(query,pair,true);
		 // assumes data types have already been specified
		tile.initializeData(pair.data, pair.attributes);
		return returnval;
	}
	
	// executes query and optionally retrieves the output
	public synchronized boolean getRawTileHelper(String query,MultiDimColumnBasedNiceTile tile, boolean retrieve_output) {
		AttributesDataPair pair = new AttributesDataPair();
		boolean returnval = getRawDataHelper(query,pair,retrieve_output);
		if(retrieve_output) {
			tile.initializeDataDefault(pair.data, pair.attributes);
		}
		return returnval;
	}
	
	public synchronized String[] buildCmdNoOutput(String query) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER="+scidbVersion+" ; "+
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; "+
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; "+
				"source ~/.bashrc ; iquery -anq \"" + query + "\"";
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
	
	// used to specify the delimiter
	public synchronized boolean getRawDataHelper(String query,AttributesDataPair pair, boolean retrieve_output) {
		return getRawDataHelper(query,pair,this.delim,retrieve_output);
	}

	// gets the raw data, and the attribute/dimension names
	public synchronized boolean getRawDataHelper(String query,AttributesDataPair pair, String delim, boolean retrieve_output) {
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
					tokens = line.split(delim,labels.size());
					for(int i = 0; i < tokens.length; i++) {
						temp.add(tokens[i]); // just add 0.0 if we can't parse it
					}
				} else {
					first = false;
					tokens = line.split(delim);
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
	
	// an iterative enumerateKeys function, instead of the recursive
	// enumerateKeys version
	// will not blow out the call stack
	public Map<MultiDimTileKey,Boolean> enumerateKeys(int[] zoomPos, double[] tileCounts) {
		double[] max = tileCounts;
		int[] curr = new int[max.length]; // all zeros
		Map<MultiDimTileKey,Boolean> masterList = new HashMap<MultiDimTileKey,Boolean>();
		
		int last = curr.length - 1;
		while(curr[last] < max[last]) {
			for(int i = 0; i < last; i++) {
				if(curr[i] >= max[i]) {
					curr[i] = 0;
					curr[i+1]++;
				}
			}
			if(curr[last] < max[last]) {
				int[] newPos = Arrays.copyOf(curr, curr.length);
				masterList.put(new MultiDimTileKey(newPos,zoomPos), true);
			}
			curr[0]++;
		}
		return masterList;
	}
	
	// returns a list of all tile keys for a given zoom level
	public Map<MultiDimTileKey,Boolean> getAllTileKeysForZoomLevel(MultiDimTileStructure ts, DimensionBoundary dimbound, int[] zoomPos) {
		double[] ranges = getRanges(dimbound);
		long[] windows = ts.getAggregationWindow(zoomPos);
		double[] tileCounts = new double[ranges.length];
		// count tiles along each dimension
		for(int j = 0; j < ranges.length; j++) { // for each dimension
			tileCounts[j] = Math.ceil(1.0 * ranges[j] / (windows[j] * ts.tileWidths[j]));
		}
		// enumerate all tiles for this zoom level
		return enumerateKeys(zoomPos, tileCounts);
	}
	
	// given boundary maps, computes the
	// range for each dimension (assuming integers)
	protected double[] getRanges(DimensionBoundary dimbound) {
		double[] ranges = new double[dimbound.dimensions.size()];
		for(int i = 0; i < ranges.length; i++) {
			String dimname = dimbound.dimensions.get(i);
			List<Long> range = dimbound.boundaryMap.get(dimname);
			long low = range.get(0);
			long high = range.get(1);
			ranges[i] = high - low + 1;
			//System.out.println("range for "+i+": "+ranges[i]);
		}
		return ranges;
	}
	
	public static void main(String[] args) {
		String query = "list('arrays')";
		MultiDimColumnBasedNiceTile tile = new MultiDimColumnBasedNiceTile();
		Scidb14_12OptimizedIqueryTileInterface sdbi = new Scidb14_12OptimizedIqueryTileInterface();
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
