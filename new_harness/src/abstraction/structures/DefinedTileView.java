package abstraction.structures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import abstraction.query.NewTileInterface;
import abstraction.query.NewTileInterface.DimensionBoundary;
import abstraction.storage.TileRetrievalHelper;
import abstraction.structures.NewTileKey.NewTileKeyJson;
import abstraction.structures.TileStructure.TileStructureJson;
import abstraction.tile.ColumnBasedNiceTile;
import abstraction.util.UtilityFunctions;


/**
 * @author leibatt
 * This class is used to get explicit tile information for the given
 * view, tiling structure, and tile interface (aka. the database connector).
 * WARNING: if you change the view or tile structure after creating the dtv,
 * then the tiles stored in the "cache_path" directory will be invalid!!!
 */
public class DefinedTileView {
	public View v;
	public TileStructure ts;
	public NewTileInterface nti;
	public SignatureMap sigMap = null; // unique to this particular defined tile view
	public TileRetrievalHelper trh = null;
	protected String sigMapFile = null;
	protected Map<NewTileKey,Boolean> allKeys = null;
	protected DimensionBoundary dimbound = null;
	protected double[][] tileCounts = null; // counts per dimension and zoom level
	
	// makes absolutely no assumptions about view or tile structure,
	// and thus everything must be passed directly.
	public DefinedTileView(View v, TileStructure ts, NewTileInterface nti,
			String sigMapFile, String cache_path) {
		this.v = v;
		this.ts = ts;
		this.nti = nti;

		this.sigMap = new SignatureMap();
		this.sigMapFile = sigMapFile;
		initializeSignatureMap(sigMapFile);
		this.trh = new TileRetrievalHelper(cache_path);
	}
	
	// assumes a generic tile structure and generates the tile structure
	public DefinedTileView(View v, NewTileInterface nti, String sigMapFile, String cache_path) {
		this(v,null,nti,sigMapFile,cache_path);
		this.ts = nti.buildDefaultTileStructure(v);
	}
	
	// assumes the same aggregation window and tile width for all dimensions.
	// useful in the case of MODIS data, where these assumptions hold true.
	// generates the tile structure given these parameters (and # of zoom levels).
	public DefinedTileView(View v, NewTileInterface nti, int aggregationWindow,
			int tileWidth, int zoomLevels, String sigMapFile, String cache_path) {
		this(v,null,nti,sigMapFile,cache_path);
		this.ts = nti.buildTileStructure(v,aggregationWindow,tileWidth,zoomLevels);
	}
	
	public void saveSignatureMap() {
		this.saveSignatureMap(this.sigMapFile);
	}
	
	public void initializeSignatureMap() {
		this.initializeSignatureMap(this.sigMapFile);
	}
	
	// gets array dimension boundaries from the tile interface
	public synchronized void getBoundaries() {
		this.dimbound = this.nti.getDimensionBoundaries(v.getQuery());
	}
	
	public String getAllTileKeysJson() {
		Map<NewTileKey,Boolean> keys = getAllTileKeys();
		NewTileKeyJson[] keyArr = new NewTileKeyJson[keys.size()];
		int i = 0;
		for(NewTileKey key : keys.keySet()) {
			keyArr[i] = new NewTileKeyJson();
			keyArr[i].dimIndices = key.dimIndices;
			keyArr[i].zoom = key.zoom;
			i++;
		}
		
		ObjectMapper o = new ObjectMapper();
		String returnval = null;
		try {
			returnval = o.writeValueAsString(keyArr);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnval;
	}
	
	// an iterative enumerateKeys function, instead of the recursive
	// enumerateKeys version
	// will not blow out the call stack
	public void enumerateKeys2(int zoom) {
		double[] max = tileCounts[zoom];
		int[] curr = new int[max.length]; // all zeros
		Map<NewTileKey,Boolean> masterList = new HashMap<NewTileKey,Boolean>();
		
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
				masterList.put(new NewTileKey(newPos,zoom), true);
			}
			curr[0]++;
		}
		this.allKeys.putAll(masterList);
	}
	
	// returns a list of all tile keys. If the list doesn't exist yet,
	// it computes the list first.
	public Map<NewTileKey,Boolean> getAllTileKeys() {
		if(this.allKeys == null) {
			this.allKeys = new HashMap<NewTileKey,Boolean>();
			getBoundaries();
			double[] ranges = getRanges();
			tileCounts = new double[this.ts.aggregationWindows.length][];
			for(int i = 0; i < this.ts.aggregationWindows.length; i++) { // for each zoom level
				int[] windows = this.ts.aggregationWindows[i];
				tileCounts[i] = new double[ranges.length];
				// count tiles along each dimension
				for(int j = 0; j < ranges.length; j++) { // for each dimension
					tileCounts[i][j] = Math.ceil(1.0 * ranges[j] / (windows[j] * this.ts.tileWidths[j]));
				}
				// enumerate all tiles for this zoom level
				//enumerateKeys(i);
				enumerateKeys2(i);
			}
		}
		return this.allKeys;
	}

	// goes over list of all keys, and finds keys within the given distance_threshold,
	// using the source key as a reference point
	public List<NewTileKey> getCandidateTileKeys(NewTileKey source, double distance_threshold) {
		List<NewTileKey> result = new ArrayList<NewTileKey>();
		getAllTileKeys();
		for(NewTileKey candidate : this.allKeys.keySet()) {
			if(UtilityFunctions.manhattanDist(source, candidate) < distance_threshold) {
				result.add(candidate);
			}
		}
		return result;
	}
	
	// is this a valid key?
	public boolean containsKey(NewTileKey key) {
		getAllTileKeys();
		return this.allKeys.containsKey(key);
	}
	
	// get the corresponding tile at the coarser zoom level
	public NewTileKey getCoarserTile(NewTileKey original, int zoomLevel) {
		return this.ts.getCoarserTile(original, zoomLevel);
	}
	
	// this function is a back door function used to quickly get tiles for
	// metadata purposes
	public ColumnBasedNiceTile getTileForMetadata(NewTileKey key) {
		ColumnBasedNiceTile tile = this.trh.getTile(key);
		if(tile == null) {
			tile = new ColumnBasedNiceTile(key);
			this.nti.getTile(this.v, this.ts, tile);
			this.trh.saveTile(tile);
		}
		return tile;
	}
	
	//note: this reset will overwrite the current sigMap file, if a save occurs
	//TODO: do something appropriate for the "cache_path" variable
	public void reset() {
		this.allKeys = null;
		this.sigMap = new SignatureMap(); // do not use the current signature map
	}
	
	//TODO: do something appropriate for the "cache_path" variable
	public void reset(String sigMapFile) {
		reset();
		this.sigMapFile = sigMapFile;
	}
	
	/************************ Helper Functions *************************/

	protected void initializeSignatureMap(String sigMapFile) {
		this.sigMap = SignatureMap.getFromFile(sigMapFile);
		if(sigMap == null) { // doesn't exist yet
			this.sigMap = new SignatureMap();
		}
	}
	
	protected void saveSignatureMap(String sigMapFile) {
		this.sigMap.save(sigMapFile);
	}
	
	//main entry point for enumerating all valid tile keys
	protected void enumerateKeys(int zoom) {
		int[] currPos = new int[tileCounts[zoom].length]; // all zeros
		Map<NewTileKey,Boolean> masterList = new HashMap<NewTileKey,Boolean>();
		enumerateKeysHelper(zoom,currPos,masterList);
		this.allKeys.putAll(masterList);
	}
	
	//  recursively generates new keys, tests if they are real, and adds them to the master list
	protected void enumerateKeysHelper(int zoom, int[] currPos, Map<NewTileKey,Boolean> masterList) {
		//System.out.println("zoom: "+zoom+",currPos: "+Arrays.toString(currPos));
		if(checkRange(currPos,zoom)) { // is this a valid key?
			NewTileKey temp = new NewTileKey(currPos, zoom);
			if(!masterList.containsKey(temp)) { // is it a new key?
				masterList.put(temp,true);
				for(int j = 0; j < currPos.length; j++) {
					int[] newPos = Arrays.copyOf(currPos, currPos.length);
					newPos[j]++;
					enumerateKeysHelper(zoom,newPos,masterList);
				}
			}
		}
	}
	
	// checks whether the proposed tile key is valid, using the computed
	// tile counts for this defined tile view
	protected boolean checkRange(int[] currPos, int zoom) {
		for(int j = 0; j < tileCounts[zoom].length; j++) {
			if(currPos[j] >= tileCounts[zoom][j]) return false;
		}
		return true;
	}
	
	// given boundary maps from the tile interface, computes the
	// range for each dimension (assuming integers)
	protected double[] getRanges() {
		double[] ranges = new double[this.dimbound.dimensions.size()];
		for(int i = 0; i < ranges.length; i++) {
			String dimname = this.dimbound.dimensions.get(i);
			List<Integer> range = this.dimbound.boundaryMap.get(dimname);
			int low = range.get(0);
			int high = range.get(1);
			ranges[i] = high - low + 1;
			//System.out.println("range for "+i+": "+ranges[i]);
		}
		return ranges;
	}
}
