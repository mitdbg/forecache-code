package backend.util;


/**
 * @author leibatt
 * Wrapper class for tile data. Stores tile ID information and data.
 * Class fields are immutable.
 */
public class Tile {
	private final TileKey id;
	private final byte[] data;
	private final int size;
	
	public Tile(TileKey id, byte[] data) {
		this.id = id;
		this.data = data;
		this.size = this.data.length;
	}
	
	public final TileKey getTileKey() {
		return this.id;
	}
	
	public int getDataSize() {
		return this.size;
	}
	
	// return a copy of the data
	public byte[] getDataCopy() {
		byte[] returnval = new byte[this.data.length];
		System.arraycopy(this.data,0,returnval,0,returnval.length);
		return returnval;
	}
	
	public double getDistance(Tile other) {
		return this.id.getDistance(other.getTileKey());
	}

}
