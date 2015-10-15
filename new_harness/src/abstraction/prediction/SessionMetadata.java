package abstraction.prediction;

/**
 * @author leibatt
 * used to store the necessary metadata for a given caching layer
 * so we can use one prediction engine to compute predictions
 * over different layers
 */
public class SessionMetadata {
	// history
	public String userid = null; // which user is this?
	public int historylength = -1; // n
	public TileHistoryQueue history = null; // last n tile requests, last ROI, etc.
	public AllocationStrategyMap allocationStrategyMap = null;
	
	public SessionMetadata(String userid, int historylength, TileHistoryQueue history,
			AllocationStrategyMap allocationStrategyMap) {
		this.userid = userid;
		this.historylength = historylength;
		this.history = history;
		this.allocationStrategyMap = allocationStrategyMap;
	}
}
