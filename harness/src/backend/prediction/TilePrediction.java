package backend.prediction;

import backend.util.TileKey;

public class TilePrediction implements Comparable<TilePrediction> {
	public TileKey id;
	public Double confidence = null;
	public Double distance = null;
	
	@Override
	public String toString() {
		return id.buildTileStringForFile() + "(" + confidence + ")";
	}
	
	public int compareConfidence(TilePrediction other) {
		double diff = this.confidence - other.confidence;
		if(diff < 0) {
			return 1;
		} else if (diff > 0) {
			return -1;
		} else {
			return 0;
		}
	}
	
	public int compareDistance(TilePrediction other) {
		double diff = this.distance - other.distance;
		if(diff < 0) {
			return -1;
		} else if (diff > 0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@Override
	public int compareTo(TilePrediction other) {
		if(confidence != null && other.confidence != null) return compareConfidence(other);
		else if (distance != null && other.distance != null) return compareDistance(other);
		else return 0; // otherwise give up
	}
}
