package implementation;

import abstraction.TileStructure;

public class MimicWaveformTileStructureFactory {
	public static String[] defaultDimensionLabels = new String[]{"msec"};
	public static int[][] defaultAggregationWindows = new int[][]{new int[]{1250},
			new int[]{625},
			new int[]{125},
			new int[]{25},
			new int[]{5},
			new int[]{1}};
	public static int[] defaultTileWidths = new int[]{100}; // one per dimension
	
	public TileStructure getDefaultMimicWaveformTileStructure() {
		TileStructure ts = new TileStructure();
		ts.aggregationWindows = defaultAggregationWindows;
		ts.tileWidths = defaultTileWidths;
		ts.dimensionLabels = defaultDimensionLabels;
		return ts;
	}
	
	public static TileStructure getMimicWaveformTileStructure(int[][] aggregationWindows,int[] tileWidths) {
		TileStructure ts = new TileStructure();
		ts.aggregationWindows = aggregationWindows;
		ts.tileWidths = tileWidths;
		ts.dimensionLabels = defaultDimensionLabels; // this is hardcoded for now
		return ts;
	}
}
