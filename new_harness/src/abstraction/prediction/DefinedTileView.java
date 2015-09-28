package abstraction.prediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import abstraction.query.NewTileInterface;
import abstraction.util.NewTileKey;
import abstraction.util.TileStructure;
import abstraction.util.UtilityFunctions;
import abstraction.util.View;


/**
 * @author leibatt
 * This class is used to get explicit tile information for the given
 * view, tiling structure, and tile interface (aka. the database connector).
 */
public class DefinedTileView {
	View v;
	TileStructure ts;
	NewTileInterface nti;
	Map<NewTileKey,Boolean> allKeys = null;
	Map<String,List<Integer>> boundaryMap = null;
	double[][] tileCounts = null; // counts per dimension and zoom level
	
	public DefinedTileView(View v, TileStructure ts, NewTileInterface nti) {
		this.v = v;
		this.ts = ts;
		this.nti = nti;
	}
	
	public synchronized void getBoundaries() {
		this.boundaryMap = this.nti.getDimensionBoundaries(v.getQuery());
	}
	
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
	
	/************************ Helper Functions *************************/
	
	protected void enumerateKeys(int zoom) {
		int[] currPos = new int[tileCounts[zoom].length]; // all zeros
		Map<NewTileKey,Boolean> masterList = new HashMap<NewTileKey,Boolean>();
		enumerateKeysHelper(zoom,currPos,masterList);
	}
	
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
	
	protected boolean checkRange(int[] currPos, int zoom) {
		for(int j = 0; j < tileCounts[zoom].length; j++) {
			if(currPos[j] >= tileCounts[zoom][j]) return false;
		}
		return true;
	}
	
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
