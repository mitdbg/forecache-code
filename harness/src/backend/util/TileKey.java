package backend.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.collect.ImmutableList;

/**
 * @author leibatt
 * Class for storing tile ID's. Also responsible for computing distance metrics.
 * Class fields are immutable
 */
public class TileKey {
	private final ImmutableList<Integer> id;
	private final ImmutableList<Double> weights;
	private final int zoom;
	
	public TileKey(List<Integer> id, int zoom) {
		this.id = new ImmutableList.Builder<Integer>()
				.addAll(id)
				.build();
		this.zoom = zoom;
		Double[] temp = new Double[this.id.size() + 1];
		Arrays.fill(temp, 1.0);
		this.weights = new ImmutableList.Builder<Double>()
				.addAll(Arrays.asList(temp))
				.build();

	}
	
	public TileKey(List<Integer> id, int zoom, List<Double> weights) {
		this.id = new ImmutableList.Builder<Integer>()
				.addAll(id)
				.build();
		this.zoom = zoom;
		// if the weights passed are unreliable, set to default
		// weights must include a value for zoom levels too
		if(weights.size() != (this.id.size() + 1)) {
			Double[] temp = new Double[this.id.size() + 1];
			Arrays.fill(temp, 1.0);
			this.weights = new ImmutableList.Builder<Double>()
					.addAll(Arrays.asList(temp))
					.build();
		} else {
			this.weights = new ImmutableList.Builder<Double>()
					.addAll(weights)
					.build();
		}
	}
	
	public ImmutableList<Integer> getID() {
		return this.id;
	}
	
	public ImmutableList<Double> getWeights() {
		return this.weights;
	}
	
	public int getZoom() {
		return this.zoom;
	}
	
	public double getDistance(TileKey other) {
		ImmutableList<Integer> oid = other.getID();
		// ignore invalid input
		if((this.id.size() != oid.size()) || (this.zoom < 0) || (other.getZoom() < 0)) {
			return -1;
		}
		return updatedEuclideanDistance(other);
	}
	
	private double updatedEuclideanDistance(TileKey other) {
		//System.out.println("comparing "+this+" and "+other);
		ImmutableList<Integer> oid = other.getID();
		List<Integer> newid = new ArrayList<Integer>();
		int zoomdiff = this.zoom - other.getZoom();
		if(zoomdiff > 0) { // this tile is at a lower zoom level
			for(int i = 0; i < this.id.size(); i++) {
				newid.add(this.id.get(i) / ((int)Math.pow(2,zoomdiff)));
				//System.out.println(newid.get(i));
			}
			//System.out.println(this.zoom);
			//System.out.println("distance: "+euclideanDistance(newid,oid,this.zoom,other.getZoom()));
			return euclideanDistance(newid,oid,this.zoom,other.getZoom());
		} else if (zoomdiff < 0) { // other is at lowewr zoom level
			zoomdiff *= -1;
			//System.out.println("zoomdiff: "+zoomdiff);
			for(int i = 0; i < this.id.size(); i++) {
				newid.add(oid.get(i) / ((int)Math.pow(2,zoomdiff)));
				//System.out.println(newid.get(i));
			}
			//System.out.println(other.getZoom());
			//System.out.println("distance: "+euclideanDistance(this.id,newid,this.zoom,other.getZoom()));
			return euclideanDistance(this.id,newid,this.zoom,other.getZoom());
		} else {
			//System.out.println("distance: "+euclideanDistance(other));
			return this.euclideanDistance(other);
		}
	}
	
	private double euclideanDistance(List<Integer> x, List<Integer> y, int zx, int zy) {
		if(x.size() != y.size()) {
			return 100000000;
		}
		double sum = 0;
		for(int i = 0; i < x.size(); i++) {
			sum += Math.pow(x.get(i) - y.get(i), 2);
		}
		sum += Math.pow(1.0 * zx - zy,2);
		return Math.sqrt(sum);
	}
	
	// computes euclidean distance over tile id's
	// treats zoom level as an additional dimension
	private double euclideanDistance(TileKey other) {
		ImmutableList<Integer> oid = other.getID();
		if(this.id.size() != oid.size()) {
			return 100000000;
		}
		double sum = 0;
		for(int i = 0; i < this.id.size(); i++) {
			sum += this.weights.get(i) * Math.pow(this.id.get(i) - oid.get(i), 2);
		}
		sum += this.weights.get(this.id.size()) * Math.pow(1.0 * this.zoom - other.getZoom(),2);
		return Math.sqrt(sum);
	}
	
	public String buildTileString() {
		StringBuilder tile_id = new StringBuilder();
		tile_id.append("[");
		if(this.id.size() > 0) {
			tile_id.append(this.id.get(0));
		} else {
			return null;
		}
		for(int i = 1; i < this.id.size(); i++) {
			tile_id.append(", ").append(this.id.get(i));
		}
		tile_id.append("]");
		return tile_id.toString();
	}
	
	public String buildTileStringForFile() {
		StringBuilder tile_id = new StringBuilder();
		if(this.id == null) {
			return null;
		}
		tile_id.append(this.zoom);
		for(int i = 0; i < this.id.size(); i++) {
			tile_id.append("_").append(this.id.get(i));
		}
		return tile_id.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("([");
		if(this.id.size() > 0) {
			sb.append(this.id.get(0));
		}
		for(int i = 1; i < this.id.size(); i++) {
			sb.append(", ").append(this.id.get(i));
		}
		sb.append("], ").append(this.zoom).append(")");
		return sb.toString();
	}
	
	//overrode for hashing in Memory Tile Buffer
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(491,37)
			.append(this.zoom);
		
		for(int i = 0; i < this.id.size(); i++) {
			hcb.append(this.id.get(i))
			.append(this.weights.get(i));
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
		if(this.id.size() != o.getID().size()) {
			return false;
		}
		ImmutableList<Integer> oid = o.getID();
		ImmutableList<Double> ow = o.getWeights();
		EqualsBuilder eb = new EqualsBuilder()
			.append(this.zoom,o.getZoom());
		for(int i = 0; i < this.id.size(); i++) {
			eb.append(this.id.get(i), oid.get(i))
			.append(this.weights.get(i), ow.get(i));
		}
		return eb.isEquals();
	}
}