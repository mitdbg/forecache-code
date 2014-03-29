package backend.prediction;

import backend.disk.DiskTileBuffer;
import backend.memory.MemoryTileBuffer;

/**
 * @author leibatt
 * Base class for creating similarity algorithms
 */
public class SimilarityThread extends Thread {
	// Shared objects:
	private final TileHistoryQueue thq; // client request history
	private final TileRecommendation tr; // stores recommendations for all similarity algorithms
	private MemoryTileBuffer mtb; // in-memory cache
	private DiskTileBuffer dtb; // disk-based cache

	
	//pass all necessary pointers in constructor
	public SimilarityThread(TileHistoryQueue thq, TileRecommendation tr, MemoryTileBuffer mtb,
			DiskTileBuffer dtb) {
		this.thq = thq;
		this.tr = tr;
		this.mtb = mtb;
		this.dtb = dtb;
	}
	
	// this is a stub
	public void run() {}

}
