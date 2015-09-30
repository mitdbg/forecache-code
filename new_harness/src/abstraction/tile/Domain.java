package abstraction.tile;

import java.util.ArrayList;
import java.util.List;

public class Domain<A extends Comparable<? super A>> {
	protected A low = null;
	protected A high = null;
	
	public void update(A value) {
		if(this.low == null) this.low = value;
		else if (this.low.compareTo(value) > 0) this.low = value;
		
		if(this.high == null) high = value;
		else if (this.high.compareTo(value) < 0) this.high = value;
	}
	
	public List<A> getDomain() {
		if((this.low != null) && (this.high != null)) {
			List<A> domain = new ArrayList<A>();
			domain.add(this.low);
			domain.add(this.high);
			return domain;
		}
		return null;
	}
}