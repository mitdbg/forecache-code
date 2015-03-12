package backend.util;

import java.io.UnsupportedEncodingException;


/**
 * @author leibatt
 * Wrapper class for tile data. Stores tile ID information and data.
 * Class fields are immutable.
 */
public class Tile implements java.io.Serializable {
	/**
	 * important for serialization
	 */
	private static final long serialVersionUID = -3991739281730186353L;
	
	public TileKey id;
	public double[] data;
	public double[] histogram = null;
	public double[] fhistogram = null;
	public double[] norm = null;
	
	public Tile() {
		this.id = null;
		this.data = null;
	}
	
	public Tile(TileKey id) {
		this();
		this.id = id;
	}
	
	public Tile(TileKey id, double[] data) {
		this.id = id;
		this.data = data;
	}
	
	public int getDataSize() {
		return this.data.length;
	}
	
	public double getDistance(Tile other) {
		return this.id.getDistance(other.id);
	}
	
	// for sending to client
	public String encodeData() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if(this.data.length > 0) builder.append(this.data[0]);
		for(int i = 1; i < this.data.length; i++) {
			builder.append(",");
			builder.append(this.data[i]);
		}
		builder.append("]");
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return this.id.toString();
	}

}
