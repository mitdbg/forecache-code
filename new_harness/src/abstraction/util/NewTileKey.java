package abstraction.util;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * @author leibatt
 * Class for storing tile ID's. Also responsible for computing distance metrics.
 * Class fields are immutable
 */
public class NewTileKey implements java.io.Serializable {
	/**
	 * important for deserialization
	 */
	private static final long serialVersionUID = -2419645717877248576L;
	
	public int[] dimIndices;
	public int zoom;
	
	public NewTileKey(int[] id, int zoom) {
		this.dimIndices = id;
		this.zoom = zoom;
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
	
	public synchronized String buildTileStringForFile() {
		StringBuilder tile_id = new StringBuilder();
		if(this.dimIndices == null) {
			return null;
		}
		for(int i = 0; i < this.dimIndices.length; i++) {
			tile_id.append(this.dimIndices[i]).append("_");
		}
		tile_id.append(this.zoom);
		return tile_id.toString();
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
		HashCodeBuilder hcb = new HashCodeBuilder(491,37)
			.append(this.zoom);
		
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
		} else if(!(other instanceof NewTileKey)) {
			return false;
		}
		
		NewTileKey o = (NewTileKey) other;
		if(this.dimIndices.length != o.dimIndices.length) {
			return false;
		}
		int[] oid = o.dimIndices;
		EqualsBuilder eb = new EqualsBuilder()
			.append(this.zoom,o.zoom);
		for(int i = 0; i < this.dimIndices.length; i++) {
			eb.append(this.dimIndices[i], oid[i]);
		}
		return eb.isEquals();
	}
}