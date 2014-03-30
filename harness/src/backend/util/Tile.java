package backend.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;


/**
 * @author leibatt
 * Wrapper class for tile data. Stores tile ID information and data.
 * Class fields are immutable.
 */
public class Tile {
	private final TileKey id;
	private final byte[] data;
	private final int size;
	private double[] histogram = null;
	private double[] fhistogram = null;
	private double[] norm = null;
	
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
	
	public double[] getNormalSignature() throws Exception {
		if(this.norm == null) {
			System.out.println("computing normal signature for"+this.id);
			this.norm = Signatures.getNormalSignature(this.data);
		}
		double[] returnval = new double[this.norm.length];
		System.arraycopy(this.norm,0,returnval,0,returnval.length);
		return returnval;
	}
	
	public double getNormalDistance(Tile other) throws Exception {
		double distance = 0;
		double [] onorm = other.getNormalSignature();
		if(this.norm == null) {
			this.norm = Signatures.getNormalSignature(this.data);
		}
		distance = Signatures.getHistogramDistance(this.norm, onorm);
		return distance;
	}
	
	public double[] getHistogramSignature() {
		if(this.histogram == null) {
			this.histogram = Signatures.getHistogramSignature(this.data);
		}
		double[] returnval = new double[this.histogram.length];
		System.arraycopy(this.histogram,0,returnval,0,returnval.length);
		return returnval;
	}
	
	public double getHistogramDistance(Tile other) {
		double distance = 0;
		double [] ohist = other.getHistogramSignature();
		if(this.histogram == null) {
			this.histogram = Signatures.getHistogramSignature(this.data);
		}
		distance = Signatures.getHistogramDistance(this.histogram, ohist);
		return distance;
	}
	
	public double[] getFilteredHistogramSignature() {
		if(this.fhistogram == null) {
			this.fhistogram = Signatures.getFilteredHistogramSignature(this.data);
		}
		double[] returnval = new double[this.fhistogram.length];
		System.arraycopy(this.fhistogram,0,returnval,0,returnval.length);
		return returnval;
	}
	
	public double getFilteredHistogramDistance(Tile other) {
		double distance = 0;
		double [] ofhist = other.getFilteredHistogramSignature();
		if(this.fhistogram == null) {
			this.fhistogram = Signatures.getFilteredHistogramSignature(this.data);
		}
		distance = Signatures.getHistogramDistance(this.fhistogram, ofhist);
		return distance;
	}
	
	public String encodeData() {
		try {
			return new String(this.data,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out.println("error occured when encoding tile data");
			e.printStackTrace();
		}
		return "";
	}
	
	@Override
	public String toString() {
		return this.id.toString();
	}

}
