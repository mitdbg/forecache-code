package abstraction.mdstructures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import abstraction.mdquery.MultiDimTileInterface;
import abstraction.mdquery.MultiDimTileInterface.DimensionBoundary;
import abstraction.mdstorage.MultiDimTileRetrievalHelper;
import abstraction.mdstructures.MultiDimTileKey.MultiDimTileKeyJson;
import abstraction.structures.View;
import abstraction.tile.MultiDimColumnBasedNiceTile;
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
	public MultiDimTileStructure ts;
	public MultiDimTileInterface nti;
	public SignatureMap sigMap = null; // unique to this particular defined tile view
	public MultiDimTileRetrievalHelper trh = null;
	protected String sigMapFile = null;
	protected Map<MultiDimTileKey,Boolean> allKeys = null;
	protected DimensionBoundary dimbound = null;
	protected Map<Integer,double[]> tileCounts = null; // counts per dimension and zoom level
	
	// makes absolutely no assumptions about view or tile structure,
	// and thus everything must be passed directly.
	public DefinedTileView(View v, MultiDimTileStructure ts, MultiDimTileInterface nti,
			String sigMapFile, String cache_path) {
		this.v = v;
		this.ts = ts;
		this.nti = nti;

		this.sigMap = new SignatureMap();
		this.sigMapFile = sigMapFile;
		initializeSignatureMap(sigMapFile);
		this.trh = new MultiDimTileRetrievalHelper(cache_path);
	}
	
	// assumes a generic tile structure and generates the tile structure
	public DefinedTileView(View v, MultiDimTileInterface nti, String sigMapFile, String cache_path) {
		this(v,null,nti,sigMapFile,cache_path);
		this.ts = nti.buildDefaultTileStructure(v);
	}
	
	// assumes the same aggregation window and tile width for all dimensions.
	// useful in the case of MODIS data, where these assumptions hold true.
	// generates the tile structure given these parameters (and # of zoom levels).
	public DefinedTileView(View v, MultiDimTileInterface nti, int aggregationWindow,
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
		Map<MultiDimTileKey,Boolean> keys = getAllTileKeys();
		MultiDimTileKeyJson[] keyArr = new MultiDimTileKeyJson[keys.size()];
		int i = 0;
		for(MultiDimTileKey key : keys.keySet()) {
			keyArr[i] = new MultiDimTileKeyJson();
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
	public void enumerateKeys2(int[] zoom) {
		HashCodeBuilder hb = new HashCodeBuilder();
		for(int i = 0; i < zoom.length; i++) {
			hb.append(zoom[i]);
		}
		int code = hb.toHashCode();
		double[] max = tileCounts.get(code);
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
				masterList.put(new MultiDimTileKey(newPos,zoom), true);
			}
			curr[0]++;
		}
		this.allKeys.putAll(masterList);
	}
	
	// returns a list of all tile keys. If the list doesn't exist yet,
	// it computes the list first.
	public Map<MultiDimTileKey,Boolean> getAllTileKeys() {
		if(this.allKeys == null) {
			this.allKeys = new HashMap<MultiDimTileKey,Boolean>();
			getBoundaries();
			double[] ranges = getRanges();
			tileCounts = new HashMap<Integer,double[]>();
			int total = 1;
			for(int i = 0; i < ts.dimensionGroups.length; i++) {
				total *= ts.dimensionGroups[i].length;
			}
			int[] zoomPos = new int[ts.dimensionGroups.length];
			for(int i = 0; i < total; i++) { // for each zoom level
				int[] windows = this.ts.getAggregationWindow(zoomPos);
				HashCodeBuilder hb = new HashCodeBuilder();
				for(int j = 0; j < zoomPos.length; j++) {
					hb.append(zoomPos[j]);
				}
				int code = hb.toHashCode();
				double[] vals = new double[ranges.length];
				// count tiles along each dimension
				for(int j = 0; j < ranges.length; j++) { // for each dimension
					vals[j] = Math.ceil(1.0 * ranges[j] / (windows[j] * this.ts.tileWidths[j]));
				}
				tileCounts.put(code,vals);
				// enumerate all tiles for this zoom level
				enumerateKeys2(Arrays.copyOf(zoomPos, zoomPos.length));
				
				// update zoom position
				zoomPos[0]++;
				for(int j = 0; j < (zoomPos.length-1); j++) {
					if(zoomPos[j] == ts.dimensionGroups[j].length) {
						zoomPos[j]--;
						zoomPos[j+1]++;
					}
				}
			}
		}
		return this.allKeys;
	}

	// goes over list of all keys, and finds keys within the given distance_threshold,
	// using the source key as a reference point
	public List<MultiDimTileKey> getCandidateTileKeys(MultiDimTileKey source, double distance_threshold) {
		List<MultiDimTileKey> result = new ArrayList<MultiDimTileKey>();
		getAllTileKeys();
		for(MultiDimTileKey candidate : this.allKeys.keySet()) {
			if(UtilityFunctions.manhattanDist(source, candidate,this.ts) < distance_threshold) {
				result.add(candidate);
			}
		}
		return result;
	}
	
	// is this a valid key?
	public boolean containsKey(MultiDimTileKey key) {
		getAllTileKeys();
		return this.allKeys.containsKey(key);
	}
	
	// this function is a back door function used to quickly get tiles for
	// metadata purposes
	public MultiDimColumnBasedNiceTile getTileForMetadata(MultiDimTileKey key) {
		MultiDimColumnBasedNiceTile tile = this.trh.getTile(key);
		if(tile == null) {
			tile = new MultiDimColumnBasedNiceTile(key);
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
