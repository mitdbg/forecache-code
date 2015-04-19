package backend.util;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author leibatt
 * Class for storing tile ID's. Also responsible for computing distance metrics.
 * Class fields are immutable
 */
public class TileKey implements java.io.Serializable {
	/**
	 * important for deserialization
	 */
	private static final long serialVersionUID = -8425373901298453080L;
	
	public int[] id;
	public double[] weights;
	public int zoom;
	
	public TileKey(int[] id, int zoom) {
		this.id = id;
		this.zoom = zoom;
		this.weights = new double[this.id.length+1];
	}
	
	public TileKey(int[] id, int zoom, double[] weights) {
		this.id = id;
		this.zoom = zoom;
		// if the weights passed are unreliable, set to default
		// weights must include a value for zoom levels too
		if(weights.length != (this.id.length + 1)) {
			this.weights = new double[this.id.length+1];
		} else {
			this.weights = weights;
		}
	}
	
	public double getDistance(TileKey other) {
		int[] oid = other.id;
		// ignore invalid input
		if((this.id.length != oid.length) || (this.zoom < 0) || (other.zoom < 0)) {
			return -1;
		}
		return updatedEuclideanDistance(other);
	}
	
	private double updatedEuclideanDistance(TileKey other) {
		//System.out.println("comparing "+this+" and "+other);
		int[] oid = other.id;
		int[] newid = new int[this.id.length];
		int zoomdiff = this.zoom - other.zoom;
		if(zoomdiff > 0) { // this tile is at a lower zoom level
			for(int i = 0; i < this.id.length; i++) {
				newid[i] = this.id[i] / ((int)Math.pow(2,zoomdiff));
				//System.out.println(newid.get(i));
			}
			//System.out.println(this.zoom);
			//System.out.println("distance: "+euclideanDistance(newid,oid,this.zoom,other.getZoom()));
			return euclideanDistance(newid,oid,this.zoom,other.zoom);
		} else if (zoomdiff < 0) { // other is at lowewr zoom level
			zoomdiff *= -1;
			//System.out.println("zoomdiff: "+zoomdiff);
			for(int i = 0; i < this.id.length; i++) {
				newid[i] = oid[i] / ((int)Math.pow(2,zoomdiff));
				//System.out.println(newid.get(i));
			}
			//System.out.println(other.getZoom());
			//System.out.println("distance: "+euclideanDistance(this.id,newid,this.zoom,other.getZoom()));
			return euclideanDistance(this.id,newid,this.zoom,other.zoom);
		} else {
			//System.out.println("distance: "+euclideanDistance(other));
			return this.euclideanDistance(other);
		}
	}
	
	private double euclideanDistance(int[] x, int[] y, int zx, int zy) {
		if(x.length != y.length) {
			return 100000000;
		}
		double sum = 0;
		for(int i = 0; i < x.length; i++) {
			sum += Math.pow(x[i] - y[i], 2);
		}
		sum += Math.pow(1.0 * zx - zy,2);
		return Math.sqrt(sum);
	}
	
	// computes euclidean distance over tile id's
	// treats zoom level as an additional dimension
	private double euclideanDistance(TileKey other) {
		int[] oid = other.id;
		if(this.id.length != oid.length) {
			return 100000000;
		}
		double sum = 0;
		for(int i = 0; i < this.id.length; i++) {
			sum += this.weights[i] * Math.pow(this.id[i]- oid[i], 2);
		}
		sum += this.weights[this.id.length] * Math.pow(1.0 * this.zoom - other.zoom,2);
		return Math.sqrt(sum);
	}
	
	public String buildTileString() {
		StringBuilder tile_id = new StringBuilder();
		tile_id.append("[");
		if(this.id.length > 0) {
			tile_id.append(this.id[0]);
		} else {
			return null;
		}
		for(int i = 1; i < this.id.length; i++) {
			tile_id.append(", ").append(this.id[i]);
		}
		tile_id.append("]");
		return tile_id.toString();
	}
	
	public synchronized String buildTileStringForFile() {
		StringBuilder tile_id = new StringBuilder();
		if(this.id == null) {
			return null;
		}
		tile_id.append(this.zoom);
		for(int i = 0; i < this.id.length; i++) {
			tile_id.append("_").append(this.id[i]);
		}
		return tile_id.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("([");
		if(this.id.length > 0) {
			sb.append(this.id[0]);
		}
		for(int i = 1; i < this.id.length; i++) {
			sb.append(", ").append(this.id[i]);
		}
		sb.append("], ").append(this.zoom).append(")");
		return sb.toString();
	}
	
	//overrode for hashing in Memory Tile Buffer
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(491,37)
			.append(this.zoom);
		
		for(int i = 0; i < this.id.length; i++) {
			hcb.append(this.id[i])
			.append(this.weights[i]);
		}
		return hcb.toHashCode();
	}
	
	//overrode for hashing in Memory Tile Buffer
	@Override
	public boolean equals(Object other) {
		if(other == null) {
			return false;
		} else if(other == this) {
			return true;
		} else if(!(other instanceof TileKey)) {
			return false;
		}
		
		TileKey o = (TileKey) other;
		if(this.id.length != o.id.length) {
			return false;
		}
		int[] oid = o.id;
		double[] ow = o.weights;
		EqualsBuilder eb = new EqualsBuilder()
			.append(this.zoom,o.zoom);
		for(int i = 0; i < this.id.length; i++) {
			eb.append(this.id[i], oid[i])
			.append(this.weights[i], ow[i]);
		}
		return eb.isEquals();
	}
}