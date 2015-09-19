package abstraction.prediction;

/**
 * @author leibatt
 * used to store the necessary metadata for a given caching layer
 * so we can use one prediction engine to compute predictions
 * over different layers
 */
public class Metadata {
	// history
	public String userid = null; // which user is this?
	public int historylength = -1; // n
	public TileHistoryQueue history = null; // last n tile requests, last ROI, etc.
	
	public Metadata(String userid, int historylength, TileHistoryQueue history) {
		this.userid = userid;
		this.historylength = historylength;
		this.history = history;
	}
}
