package backend.prefetch;

import backend.util.Direction;

public class DirectionPrediction implements Comparable<DirectionPrediction> {
	public Direction d;
	public double confidence;
	
	@Override
	public String toString() {
		return d + "(" + confidence + ")";
	}
	
	@Override
	public int compareTo(DirectionPrediction other) {
		double diff = this.confidence - other.confidence;
		if(diff < 0) {
			return -1;
		} else if (diff > 0) {
			return 1;
		} else {
			return 0;
		}
	}
}
