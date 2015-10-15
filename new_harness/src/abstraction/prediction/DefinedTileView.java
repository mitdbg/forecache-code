package abstraction.prediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import abstraction.query.NewTileInterface;
import abstraction.util.NewTileKey;
import abstraction.util.SignatureMap;
import abstraction.util.TileStructure;
import abstraction.util.UtilityFunctions;
import abstraction.util.View;


/**
 * @author leibatt
 * This class is used to get explicit tile information for the given
 * view, tiling structure, and tile interface (aka. the database connector).
 */
public class DefinedTileView {
	public View v;
	public TileStructure ts;
	public NewTileInterface nti;
	public SignatureMap sigMap = null; // unique to this particular defined tile view
	protected String sigMapFile = null;
	protected Map<NewTileKey,Boolean> allKeys = null;
	protected Map<String,List<Integer>> boundaryMap = null;
	protected double[][] tileCounts = null; // counts per dimension and zoom level
	
	public DefinedTileView(View v, TileStructure ts, NewTileInterface nti,
			String sigMapFile) {
		this.v = v;
		this.ts = ts;
		this.nti = nti;

		this.sigMap = new SignatureMap();
		this.sigMapFile = sigMapFile;
		initializeSignatureMap(sigMapFile);
	}
	
	public void saveSignatureMap() {
		this.saveSignatureMap(this.sigMapFile);
	}
	
	public void initializeSignatureMap() {
		this.initializeSignatureMap(this.sigMapFile);
	}
	
	// gets array dimension boundaries from the tile interface
	public synchronized void getBoundaries() {
		this.boundaryMap = this.nti.getDimensionBoundaries(v.getQuery());
	}
	
	// returns a list of all tile keys. If the list doesn't exist yet,
	// it computes the list first.
	public Map<NewTileKey,Boolean> getAllTileKeys() {
		if(this.allKeys == null) {
			getBoundaries();
			double[] ranges = getRanges();
			tileCounts = new double[this.ts.aggregationWindows.length][];
			for(int i = 0; i < this.ts.aggregationWindows.length; i++) { // for each zoom level
				int[] windows = this.ts.aggregationWindows[i];
				tileCounts[i] = new double[ranges.length];
				// count tiles along each dimension
				for(int j = 0; j < ranges.length; j++) { // for each dimension
					tileCounts[i][j] = Math.ceil(1.0 * ranges[j] / windows[j]);
				}
				// enumerate all tiles for this zoom level
				enumerateKeys(i);
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
		return this.allKeys.containsKey(key);
	}
	
	// get the corresponding tile at the coarser zoom level
	public NewTileKey getCoarserTile(NewTileKey original, int zoomLevel) {
		return this.ts.getCoarserTile(original, zoomLevel);
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
	}
	
	//  recursively generates new keys, tests if they are real, and adds them to the master list
	protected void enumerateKeysHelper(int zoom, int[] currPos, Map<NewTileKey,Boolean> masterList) {
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
	// ranges of 
	protected double[] getRanges() {
		double[] ranges = new double[this.ts.dimensionLabels.length];
		for(int i = 0; i < this.ts.dimensionLabels.length; i++) {
			String dimname = this.ts.dimensionLabels[i];
			List<Integer> range = this.boundaryMap.get(dimname);
			int low = range.get(0);
			int high = range.get(1);
			ranges[i] = high - low + 1;
		}
		return ranges;
	}
}
