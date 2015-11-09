package implementation.modis;

import abstraction.structures.TileStructure;

public class OldModisTileStructureFactory {
	//public static String[] defaultDimensionLabels = new String[]{"msec"};
	
	// taken from parameters file from old setup
	// Note: zoom levels 1 and zero were computed incorrectly in the orignial parameters data.
	// carried over here, as best I could manage.
	public static int[][] defaultAggregationWindows = new int[][]{
		new int[]{150,150},
		new int[]{98,98},
		new int[]{64,64},
		new int[]{32,32},
		new int[]{16,16},
		new int[]{8,8},
		new int[]{4,4},
		new int[]{2,2},
		new int[]{1,1}};
	public static int[] defaultTileWidths = new int[]{300,300}; // one per dimension
	
	public static TileStructure getDefaultModisTileStructure() {
		TileStructure ts = new TileStructure();
		ts.aggregationWindows = defaultAggregationWindows;
		ts.tileWidths = defaultTileWidths;
		//ts.dimensionLabels = defaultDimensionLabels;
		return ts;
	}
	
	public static TileStructure getModisTileStructure(int[][] aggregationWindows,int[] tileWidths) {
		TileStructure ts = new TileStructure();
		ts.aggregationWindows = aggregationWindows;
		ts.tileWidths = tileWidths;
		//ts.dimensionLabels = defaultDimensionLabels; // this is hardcoded for now
		return ts;
	}
}
