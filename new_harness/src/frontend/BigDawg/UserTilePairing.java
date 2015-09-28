package frontend.BigDawg;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class UserTilePairing {
	public String user;
	public String tile_id;
	
	// must be initialized by caller
	public UserTilePairing() {
		this.user = "";
		this.tile_id = "";
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null) {
			return false;
		} else if(o == this) {
			return true;
		} else if(!(o instanceof UserTilePairing)) {
			return false;
		}
		UserTilePairing other = (UserTilePairing) o;
		EqualsBuilder eb = new EqualsBuilder()
		.append(this.user,other.user);
		eb.append(this.tile_id, other.tile_id);
		return eb.isEquals();
	}
	
	//overrode for hashing in Memory Tile Buffer
	@Override
	public int hashCode() {
		HashCodeBuilder hcb = new HashCodeBuilder(491,37)
			.append(this.user).append(this.tile_id);
		return hcb.toHashCode();
	}
}
