package abstraction.mdstructures;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author leibatt
 * This class is the multidimensional version of its predecessor (TileStructure.java).
 * This class supports dimension groups (collections of dimensions that share zoom levels).
 * Note: the dimension names are irrelevant to the tile structure, and restrict
 * the tile structure from being reusable across queries
 */
public class MultiDimTileStructure implements java.io.Serializable {
	private static final long serialVersionUID = -393799966449384155L;
	public long[][][] aggregationWindows; // length = total dimension groups
										// subarray length = total zoom levels for this dim group
										// sub-subarray length = # dimensions in dimgroup
	public int[][] dimensionGroups;
	public long[] tileWidths; // one per dimension
	protected int[] dimensionPositions = null;
	
	/**
	 * For a zoom id, return the corresponding aggregation windows. Supports dimension groups.
	 * @param zoomPos a zoom level represented as an array, where each cell 
	 * is a zoom level for each dimension group in the tile structure
	 * @return the aggregation windows for each dimension, for this zoom level
	 */
	public long[] getAggregationWindow(int[] zoomPos) {
		long[] sortedWindows = new long[tileWidths.length];
		getDimensionPositions();
		int index = 0;
		for(int i = 0; i < zoomPos.length; i++) {
			long[] windows = aggregationWindows[i][zoomPos[i]];
			for(int j = 0; j < windows.length; j++) {
				sortedWindows[dimensionPositions[index]] = windows[j];
				index++;
			}
		}
		// need one window per zoom level
		return sortedWindows;
	}
	
	/**
	 * given an old zoom position and new zoom position, calculate the new midpoint for the
	 * new zoom position using the old midpoint as reference.
	 * @param oldZoomPos old zoom position with existing midpoint
	 * @param newZoomPos new zoom position to get the new midpoint for
	 * @param oldMid existing midpoint, array with length equal to number of dimensions
	 * @return the new midpoint
	 */
	public double[] getNewMid(int[] oldZoomPos, int[] newZoomPos, double[] oldMid) {
		getDimensionPositions();
		long[] newWindows = getAggregationWindow(newZoomPos); // windows for each dimension at this zoom level
		long[] oldWindows = getAggregationWindow(oldZoomPos);
		
		double[] newMid = new double[oldMid.length];
		for(int i = 0; i < oldMid.length; i++) { // for each dimenison
			newMid[i] = oldMid[i] * oldWindows[i] / newWindows[i];
		}
		
		return newMid;
	}
	
	/**
	 * takes a tile and finds the corresponding parent tile at the given zoom level. First
	 * computes the midpoint of the tile, then translates this midpoint to the desired zoom level.
	 * Then calculates which tile this point resides in at the higher zoom level.
	 * @param k the key for the tile to translate
	 * @param newZoomPos the parent zoom level to get the parent tile from
	 * @return the key of the parent tile
	 */
	public MultiDimTileKey getParentTile(MultiDimTileKey k, int[] newZoomPos) {
		double[] oldMid = getMidpointForTile(k);
		double[] newMid = getNewMid(k.zoom,newZoomPos,oldMid);
		return getTileForPoint(newZoomPos,newMid);
	}
	
	/**
	 * compute the midpoint (i.e., center value along each dimension) for the given tile
	 * @param k the key for this particular tile
	 * @return the midpoint of the tile
	 */
	public double[] getMidpointForTile(MultiDimTileKey k) {
		double[] mid = new double[k.dimIndices.length];
		for(int i = 0; i < mid.length; i++) { // for each dimension
			mid[i] = (k.dimIndices[i]*1.0 + 0.5)*tileWidths[i]; // compute the tile id along this dimension
		}
		return mid;
	}
	
	/**
	 * Computes the corresponding tile key for a given zoom position and point
	 * @param zoomPos the zoom level of the midpoint
	 * @param point the point to calculate the corresponding tile for
	 * @return the key of the tile that owns the point
	 */
	public MultiDimTileKey getTileForPoint(int[] zoomPos, double[] point) {
		MultiDimTileKey k = new MultiDimTileKey();
		k.zoom = Arrays.copyOf(zoomPos, zoomPos.length);
		k.dimIndices = new int[point.length];
		for(int i = 0; i < point.length; i++) { // for each dimension
			k.dimIndices[i] = (int) Math.floor(point[i] / tileWidths[i]); // compute the tile id along this dimension
		}
		return k;
	}
	
	/**
	 * Creates a JSON version of the tile structure, in case the client
	 * needs it to track user stuff.
	 * @return the tile structure as a json object
	 */
	public String toJson() {
		return toJson(getMDTSJson());
		
	}
	
	/**
	 * creates a java multidim tile structure from a JSON string.
	 * @param jsonstring the JSON string to parse
	 * @return whether the parse was successful or not
	 */
	public boolean fromJson(String jsonstring) {
		MultiDimTileStructureJson mdtsjson = null;
		ObjectMapper o = new ObjectMapper();
		try {
			mdtsjson = o.readValue(jsonstring, MultiDimTileStructureJson.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(mdtsjson != null) {
			this.aggregationWindows = mdtsjson.aggregationWindows;
			//this.dimensionLabels = tsjson.dimensionLabels;
			this.tileWidths = mdtsjson.tileWidths;
			this.dimensionGroups = mdtsjson.dimensionGroups;
			this.dimensionPositions = null;
			return true;
		}
		return false;
	}
	
	/****************** Helper Functions *********************/
	protected MultiDimTileStructureJson getMDTSJson() {
		MultiDimTileStructureJson mdtsjson = new MultiDimTileStructureJson();
		mdtsjson.aggregationWindows = this.aggregationWindows;
		//tsjson.dimensionLabels = this.dimensionLabels;
		mdtsjson.tileWidths = this.tileWidths;
		mdtsjson.dimensionGroups = this.dimensionGroups;
		return mdtsjson;
	}
	
	protected String toJson(MultiDimTileStructureJson tsjson) {
		ObjectMapper o = new ObjectMapper();
		String returnval = null;
		try {
			returnval = o.writeValueAsString(tsjson);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnval;
	}
	
	protected void getDimensionPositions() {
		if(dimensionPositions != null) return;
		dimensionPositions = new int[tileWidths.length];
		int index = 0;
		for(int i = 0; i < dimensionGroups.length; i++) {
			int[] group = dimensionGroups[i];
			for(int j = 0; j < group.length; j++) {
				dimensionPositions[index] = group[j];
				index++;
			}
		}
	}
	
	/****************** Nested Classes *********************/
	public static class MultiDimTileStructureJson implements java.io.Serializable {
		private static final long serialVersionUID = -1685745339476813442L;
		public long[][][] aggregationWindows; // length = total dimension groups
											// subarray length = total zoom levels for this dim group
											// sub-subarray length = # dimensions in dimgroup
		public int[][] dimensionGroups;
		public long[] tileWidths; // one per dimension
	}
}
