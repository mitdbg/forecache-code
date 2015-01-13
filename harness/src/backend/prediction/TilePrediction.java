package backend.prediction;

import backend.util.TileKey;

public class TilePrediction implements Comparable<TilePrediction> {
	public TileKey id;
	public Double confidence = null;
	public Double distance = null;
	public Double physicalDistance = 1.0;
	public boolean useDistance = false;
	public double base = 10;
	
	@Override
	public String toString() {
		return id.buildTileStringForFile() + "(" + confidence+","+distance + ")";
	}
	
	public int compareConfidence(TilePrediction other) {
		double a = this.confidence;
		double b = other.confidence;
		double diff = 0;
		
		if(useDistance) {
			if(this.confidence < 0.0) { // log
				a -= physicalDistance-1;
			} else {
				a /= Math.pow(base,(physicalDistance-1)); // worse if far away
			}
			
			if(this.confidence < 0.0) { // log
				b -= other.physicalDistance-1;
			} else {
				b /= Math.pow(base,(other.physicalDistance-1)); // worse if far away
			}
		}
		
		diff = a-b;
		
		if(diff < 0) {
			return 1;
		} else if (diff > 0) {
			return -1;
		} else {
			return 0;
		}
	}
	
	public int compareDistance(TilePrediction other) {
		double a = this.distance;
		double b = other.distance;
		double diff = 0;
		
		if(useDistance) {
			a *= Math.pow(base,(physicalDistance-1)); // worse if far away
			b *= Math.pow(base,(other.physicalDistance-1)); // worse if far away
		}
		
		diff = a-b;
		
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
