package implementation.mdmodis;

import abstraction.mdstructures.MultiDimTileStructure;

public class ModisTileStructureFactory {
	//public static String[] defaultDimensionLabels = new String[]{"msec"};
	
	// taken from parameters file from old setup
	// Note: zoom levels 1 and zero were computed incorrectly in the orignial parameters data.
	// carried over here, as best I could manage.
	public static long[][][] defaultAggregationWindows = new long[][][]{{
		new long[]{150,150},
		new long[]{98,98},
		new long[]{64,64},
		new long[]{32,32},
		new long[]{16,16},
		new long[]{8,8},
		new long[]{4,4},
		new long[]{2,2},
		new long[]{1,1}}};
	public static int[][] defaultDimensionGroups = new int[][]{{0,1}};
	public static long[] defaultTileWidths = new long[]{300,300}; // one per dimension
	
	public static MultiDimTileStructure getDefaultModisTileStructure() {
		MultiDimTileStructure ts = new MultiDimTileStructure();
		ts.aggregationWindows = defaultAggregationWindows;
		ts.tileWidths = defaultTileWidths;
		ts.dimensionGroups = defaultDimensionGroups;
		//ts.dimensionLabels = defaultDimensionLabels;
		return ts;
	}
	
	public static MultiDimTileStructure getModisTileStructure(long[][][] aggregationWindows,int[][] dimensionGroups,long[] tileWidths) {
		MultiDimTileStructure ts = new MultiDimTileStructure();
		ts.aggregationWindows = aggregationWindows;
		ts.dimensionGroups = dimensionGroups;
		ts.tileWidths = tileWidths;
		//ts.dimensionLabels = defaultDimensionLabels; // this is hardcoded for now
		return ts;
	}
}
