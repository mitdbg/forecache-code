package abstraction.structures;

import java.util.Arrays;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author leibatt
 * Class for storing tile ID's. Also responsible for computing distance metrics.
 * Class fields are immutable
 */
public class MultiDimTileKey implements java.io.Serializable {
	/**
	 * important for deserialization
	 */
	private static final long serialVersionUID = -2661846343416640737L;
	
	public int[] dimIndices = null;
	public int[] zoom = null; // refers to dimension groups
	private String name = null;
	
	public MultiDimTileKey() {}
	
	public MultiDimTileKey(int[] id, int[] zoom) {
		this.dimIndices = id;
		this.zoom = zoom;
	}
	
	public MultiDimTileKey copy() {
		MultiDimTileKey copy = new MultiDimTileKey(
				Arrays.copyOf(this.dimIndices, this.dimIndices.length),
				Arrays.copyOf(this.zoom, this.zoom.length));
		return copy;
	}
	
	public String buildTileString() {
		StringBuilder tile_id = new StringBuilder();
		tile_id.append("[");
		if(this.dimIndices.length > 0) {
			tile_id.append(this.dimIndices[0]);
		} else {
			return null;
		}
		for(int i = 1; i < this.dimIndices.length; i++) {
			tile_id.append(", ").append(this.dimIndices[i]);
		}
		tile_id.append("]");
		return tile_id.toString();
	}
	
	// pretty much the name of the tile key
	public synchronized String buildTileStringForFile() {
		if(this.name == null) {
			StringBuilder tile_id = new StringBuilder();
			if(this.dimIndices == null || this.zoom == null) {
				return null;
			}
	
			tile_id.append("_zoom");
			for(int i = 0; i < this.zoom.length; i++) {
				tile_id.append("_").append(this.zoom[i]);
			}
			tile_id.append("_pos");
			for(int i = 0; i < this.dimIndices.length; i++) {
				tile_id.append("_").append(this.dimIndices[i]);
			}
			this.name = tile_id.toString();
		}
		return name;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("([");
		if(this.dimIndices.length > 0) {
			sb.append(this.dimIndices[0]);
		}
		for(int i = 1; i < this.dimIndices.length; i++) {
			sb.append(", ").append(this.dimIndices[i]);
		}
		sb.append("], ").append(this.zoom).append(")");
		return sb.toString();
	}
	
	//overrode for hashing in Memory Tile Buffer
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(491,37);
	
		for(int i = 0; i < this.zoom.length; i++) {
			hcb.append(this.zoom[i]);
		}
		
		for(int i = 0; i < this.dimIndices.length; i++) {
			hcb.append(this.dimIndices[i]);
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
		} else if(!(other instanceof MultiDimTileKey)) {
			return false;
		}
		
		MultiDimTileKey o = (MultiDimTileKey) other;
		if(this.dimIndices.length != o.dimIndices.length) {
			return false;
		}
		if(this.zoom.length != o.zoom.length) {
			return false;
		}
		int[] oid = o.dimIndices;
		int[] zid = o.zoom;
		EqualsBuilder eb = new EqualsBuilder();
		
		for(int i = 0; i < this.zoom.length; i++) {
			eb.append(this.zoom[i], zid[i]);
		}
		
		for(int i = 0; i < this.dimIndices.length; i++) {
			eb.append(this.dimIndices[i], oid[i]);
		}
		return eb.isEquals();
	}
	
	/****************** Nested Classes *********************/
	public static class MultiDimTileKeyJson implements java.io.Serializable {
		private static final long serialVersionUID = -8569013859563297087L;
		public int[] dimIndices;
		public int[] zoom;
	}
}