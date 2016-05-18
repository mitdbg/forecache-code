package implementation.mdmodis;

import abstraction.mdstructures.MultiDimTileStructure;

public class ModisTileStructureFactory {
	//public static String[] defaultDimensionLabels = new String[]{"msec"};
	
	// taken from parameters file from old setup
	// Note: zoom levels 1 and zero were computed incorrectly in the orignial parameters data.
	// carried over here, as best I could manage.
	public static int[][][] defaultAggregationWindows = new int[][][]{{
		new int[]{150,150},
		new int[]{98,98},
		new int[]{64,64},
		new int[]{32,32},
		new int[]{16,16},
		new int[]{8,8},
		new int[]{4,4},
		new int[]{2,2},
		new int[]{1,1}}};
	public static int[][] defaultDimensionGroups = new int[][]{{0,1}};
	public static int[] defaultTileWidths = new int[]{300,300}; // one per dimension
	
	public static MultiDimTileStructure getDefaultModisTileStructure() {
		MultiDimTileStructure ts = new MultiDimTileStructure();
		ts.aggregationWindows = defaultAggregationWindows;
		ts.tileWidths = defaultTileWidths;
		ts.dimensionGroups = defaultDimensionGroups;
		//ts.dimensionLabels = defaultDimensionLabels;
		return ts;
	}
	
	public static MultiDimTileStructure getModisTileStructure(int[][][] aggregationWindows,int[][] dimensionGroups,int[] tileWidths) {
		MultiDimTileStructure ts = new MultiDimTileStructure();
		ts.aggregationWindows = aggregationWindows;
		ts.dimensionGroups = dimensionGroups;
		ts.tileWidths = tileWidths;
		//ts.dimensionLabels = defaultDimensionLabels; // this is hardcoded for now
		return ts;
	}
}
