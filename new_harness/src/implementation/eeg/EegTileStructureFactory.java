package implementation.eeg;

import abstraction.structures.TileStructure;

public class EegTileStructureFactory {
	//public static String[] defaultDimensionLabels = new String[]{"msec"};
	
	// taken from parameters file from old setup
	// Note: zoom levels 1 and zero were computed incorrectly in the orignial parameters data.
	// carried over here, as best I could manage.
	public static int[][] defaultAggregationWindows = new int[][]{
		new int[]{1,4},
		new int[]{1,2},
		new int[]{1,1}};
	public static int[] defaultTileWidths = new int[]{83,83}; // one per dimension
	
	public static TileStructure getDefaultTileStructure() {
		TileStructure ts = new TileStructure();
		ts.aggregationWindows = defaultAggregationWindows;
		ts.tileWidths = defaultTileWidths;
		//ts.dimensionLabels = defaultDimensionLabels;
		return ts;
	}
	
	public static TileStructure getTileStructure(int[][] aggregationWindows,int[] tileWidths) {
		TileStructure ts = new TileStructure();
		ts.aggregationWindows = aggregationWindows;
		ts.tileWidths = tileWidths;
		//ts.dimensionLabels = defaultDimensionLabels; // this is hardcoded for now
		return ts;
	}
}
