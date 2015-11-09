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
 * Note: the dimension names are irrelevant to the tile structure, and restrict
 * the tile structure from being reusable across queries
 */
public class TileStructure implements java.io.Serializable {
	private static final long serialVersionUID = -7588792485497184441L;
	//public String[] dimensionLabels; // need to be in dimension order
	public int[][] aggregationWindows; // length = total zoom levels, length of subarray = # dimensions
	public int []tileWidths; // one per dimension
	
	//for a zoom id, return the corresponding aggregation window
	public int[] getAggregationWindow(int zoom) {
		return Arrays.copyOf(aggregationWindows[zoom], aggregationWindows[zoom].length);
	}
	
	// for the given aggregation window, figure out its zoom id
	public int getZoomLevel(int[] aggregationWindow) {
		if(aggregationWindows.length == 0) return -1;
		else if(aggregationWindow.length != aggregationWindows[0].length) return -1;
		for(int i = 0; i < aggregationWindows.length; i++) {
			int found = 0;
			for(int d = 0; d < aggregationWindow.length; d++) {
				if(aggregationWindow[d] == aggregationWindows[i][d]) found++;
			}
			if(found == aggregationWindow.length) return i;
		}
		return -1;
	}
	
	public NewTileKey getCoarserTile(NewTileKey original, int zoom) {
		if(zoom < original.zoom) {
			NewTileKey parent = original.copy();
			parent.zoom = zoom;
			// calculate the raw data position
			for(int i = 0; i < parent.dimIndices.length; i++) {
				// unroll the tile widths
				// tile id -> position in aggregated points
				parent.dimIndices[i] *= tileWidths[i];

				// unroll the aggregation widths
				// position in aggregated points -> position in raw data points
				parent.dimIndices[i] *= aggregationWindows[original.zoom][i];
				
				// calculate the proper position at the coarser zoom level
				parent.dimIndices[i] /= aggregationWindows[parent.zoom][i];
			}
			return parent;
		}
		return original;
	}
	
	// returns the structure as a json object
	// in case the client needs it to track user stuff
	public String toJson() {
		return toJson(getTSJson());
		
	}
	
	public boolean fromJson(String jsonstring) {
		TileStructureJson tsjson = null;
		ObjectMapper o = new ObjectMapper();
		try {
			tsjson = o.readValue(jsonstring, TileStructureJson.class);
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
	
	/*
	// data for the front-end to setup its tile tracking
	public String getTileStructureJson() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
	
		
		// dimension labels
		sb.append("\"dimensionLabels\":[");
		if(dimensionLabels.length > 0) {
			sb.append(dimensionLabels[0]);
			for(int i = 1; i < dimensionLabels.length; i++) {
				sb.append(",").append(dimensionLabels[i]);
			}
		}
		sb.append("]");
		
		//aggregationWindows
		sb.append(",");
		sb.append("\"aggregationWidnows\":{");
		if(aggregationWindows.length > 0) {
			int[] widths = aggregationWindows[0];
			if(widths.length > 0) {
				sb.append("{");
				sb.append("\""+dimensionLabels[0]+"\"").append(":").append(widths[0]);
				for(int d = 1; d < widths.length; d++) {
					sb.append(",").append("\""+dimensionLabels[d]+"\"").append(":").append(widths[d]);
				}
				sb.append("}");
				for(int i = 1; i < dimensionLabels.length; i++) {
					widths = aggregationWindows[i];
					sb.append("{");
					sb.append("\""+dimensionLabels[0]+"\"").append(":").append(widths[0]);
					for(int d = 1; d < widths.length; d++) {
						sb.append(",").append("\""+dimensionLabels[d]+"\"").append(":").append(widths[d]);
					}
					sb.append("}");
				}
			}
		}
		sb.append("}");
		
		//tile widths
		sb.append("\"tileWidths\":[");
		if(tileWidths.length > 0) {
			sb.append(tileWidths[0]);
			for(int d = 1; d < tileWidths.length; d++) {
				sb.append(",").append(tileWidths[d]);
			}
		}
		sb.append("]");
		
		sb.append("}");
		
		return sb.toString();
	}
	*/
	
	/****************** Helper Functions *********************/
	protected TileStructureJson getTSJson() {
		TileStructureJson tsjson = new TileStructureJson();
		tsjson.aggregationWindows = this.aggregationWindows;
		//tsjson.dimensionLabels = this.dimensionLabels;
		tsjson.tileWidths = this.tileWidths;
		return tsjson;
	}
	
	protected String toJson(TileStructureJson tsjson) {
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
	
	/****************** Nested Classes *********************/
	public static class TileStructureJson implements java.io.Serializable {
		private static final long serialVersionUID = -7275959924981908343L;
		//public String[] dimensionLabels; // need to be in dimension order
		public int[][] aggregationWindows; // length = total zoom levels, length of subarray = # dimensions
		public int []tileWidths; // one per dimension
	}
}
