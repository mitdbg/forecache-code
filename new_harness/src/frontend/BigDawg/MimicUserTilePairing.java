package frontend.BigDawg;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class MimicUserTilePairing {
	public String user;
	public String recordName;
	public int aggWindow;
	public int k;
	public int tile_id;
	
	// must be initialized by caller
	public MimicUserTilePairing() {
		this.user = "";
		this.recordName = "";
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) {
			return false;
		} else if(o == this) {
			return true;
		} else if(!(o instanceof MimicUserTilePairing)) {
			return false;
		}
		MimicUserTilePairing other = (MimicUserTilePairing) o;
		EqualsBuilder eb = new EqualsBuilder()
			.append(this.user,other.user)
			.append(this.aggWindow,other.aggWindow)
			.append(this.k, other.k)
			.append(this.recordName,other.recordName)
			.append(this.tile_id, other.tile_id);
		return eb.isEquals();
	}
	
	//overrode for hashing in Memory Tile Buffer
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(491,37)
			.append(this.user)
			.append(this.tile_id)
			.append(this.aggWindow)
			.append(this.k)
			.append(this.recordName);
		return hcb.toHashCode();
	}
}
