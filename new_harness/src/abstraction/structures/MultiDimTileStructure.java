package abstraction.structures;

import java.io.IOException;
import java.util.Arrays;


import abstraction.query.BigDawgScidbTileInterface.BigDawgResponseObject;

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
	public int[][][] aggregationWindows; // length = total dimension groups
										// subarray length = total zoom levels for this dim group
										// sub-subarray length = # dimensions in dimgroup
	public int[][] dimensionGroups;
	public int[] tileWidths; // one per dimension
	protected int[] dimensionPositions = null;
	
	/**
	 * For a zoom id, return the corresponding aggregation windows. Supports dimension groups.
	 * @param zoomPos a zoom level represented as an array, where each cell 
	 * is a zoom level for each dimension group in the tile structure
	 * @return the aggregation windows for each dimension, for this zoom level
	 */
	public int[] getAggregationWindow(int[] zoomPos) {
		int[] sortedWindows = new int[tileWidths.length];
		getDimensionPositions();
		int index = 0;
		for(int i = 0; i < zoomPos.length; i++) {
			int[] windows = aggregationWindows[i][zoomPos[i]];
			for(int j = 0; j < windows.length; j++) {
				sortedWindows[dimensionPositions[index]] = windows[j];
				index++;
			}
		}
		// need one window per zoom level
		return sortedWindows;
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
		MultiDimTileStructureJson tsjson = null;
		ObjectMapper o = new ObjectMapper();
		try {
			tsjson = o.readValue(jsonstring, MultiDimTileStructureJson.class);
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
		if(tsjson != null) {
			this.aggregationWindows = tsjson.aggregationWindows;
			//this.dimensionLabels = tsjson.dimensionLabels;
			this.tileWidths = tsjson.tileWidths;
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
		public int[][][] aggregationWindows; // length = total dimension groups
											// subarray length = total zoom levels for this dim group
											// sub-subarray length = # dimensions in dimgroup
		public int[][] dimensionGroups;
		public int[] tileWidths; // one per dimension
	}
}
