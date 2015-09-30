package backend.prediction;

import abstraction.util.DistanceModifiers;
import abstraction.util.Model;
import abstraction.util.NewTileKey;

public class TilePrediction implements Comparable<TilePrediction> {
	public NewTileKey id;
	public Double confidence = null;
	public Double distance = null;
	public Double physicalDistance = 1.0;
	public boolean useDistance = false;
	public double base = 0.5;
	protected Model m;
	
	public TilePrediction(Model m) {
		this.m = m;
		this.base = DistanceModifiers.getBase(m);
	}
	
	@Override
	public String toString() {
		if(distance != null) {
			return id.buildTileStringForFile() + "(" + "model("+this.m+")"+","+confidence+","+distance+","+"base("+base+")"+",ndist="+(distance*Math.pow(1.0/base,(this.physicalDistance-1))) + ")";
		} else if (confidence < 0.0) {
			return id.buildTileStringForFile() + "(" + "model("+this.m+")"+","+confidence+","+distance+","+"base("+base+")"+",nconf="+(confidence*Math.pow(base,(this.physicalDistance-1))) + ")";
		} else {
			return id.buildTileStringForFile() + "(" + "model("+this.m+")"+","+confidence+","+distance+","+"base("+base+")"+",nconf="+(confidence+Math.log(Math.pow(base,(this.physicalDistance-1)))) + ")";
		}
	}
	
	public int compareConfidence(TilePrediction other) {
		double a = this.confidence;
		double b = other.confidence;
		double diff = 0;
		
		if(useDistance) {
			if(this.confidence < 0.0) { // log
				a += Math.log(Math.pow(base,(physicalDistance-1)));
			} else {
				a *= Math.pow(base,(physicalDistance-1)); // worse if far away
			}
			
			if(this.confidence < 0.0) { // log
				b += Math.log(Math.pow(base,(physicalDistance-1)));
			} else {
				b *= Math.pow(base,(other.physicalDistance-1)); // worse if far away
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
			a *= Math.pow(1.0/base,(physicalDistance-1)); // worse if far away
			b *= Math.pow(1.0/base,(other.physicalDistance-1)); // worse if far away
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
