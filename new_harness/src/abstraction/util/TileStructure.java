package abstraction.util;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TileStructure implements java.io.Serializable {
	private static final long serialVersionUID = -7588792485497184441L;
	public String[] dimensionLabels; // need to be in dimension order
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
			for(int j = 0; j < aggregationWindow.length; j++) {
				if(aggregationWindow[j] == aggregationWindows[i][j]) found++;
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
		ObjectMapper o = new ObjectMapper();
		return "";
	}
}
