package backend.util;

import java.util.List;

public class NiceTile implements java.io.Serializable {
	/**
	 * important for deserialization
	 */
	private static final long serialVersionUID = 543885535684644738L;
	
	public TileKey id;
	public double[] data;
	public double[][] extrema = null;
	public String[] attributes = null;
	public double[] norm = null;
	public double[] hist = null;
	public double[] fhist = null;
	
	public NiceTile() {
		this.id = null;
		this.data = null;
	}
	
	public NiceTile(TileKey id) {
		this();
		this.id = id;
	}
	
	public NiceTile(TileKey id, double[] data) {
		this.id = id;
		this.data = data;
	}
	
	public void initializeData(List<Double> data,String[] attr) {
		this.extrema = new double[attr.length][];
		for(int i = 0; i < attr.length; i++) {
			extrema[i] = null;
		}
		this.attributes = attr;
		this.data = new double[data.size()];
		int col = 0;
		for(int i = 0; i < data.size(); i++) {
			this.data[i] = data.get(i).doubleValue();
			double[] mm = extrema[col];
			double val = this.data[i];
			if(mm == null) {
				mm = new double[2];
				mm[0] = val;
				mm[1] = val;
				extrema[i] = mm;
			} else {
				if (val < mm[0]) {
					mm[0] = val;
				}
				if(mm == null || val > mm[1]) {
					mm[1] = val;
				}
			}
			col++;
			if(col >= attr.length) {
				col = 0;
			}
		}
	}
	
	public int getIndex(String name) {
		for(int i = 0; i < attributes.length; i++) {
			if(name.equals(attributes[i])) return i;
		}
		return -1;
	}
	
	// index1 = column, index2 = row
	public double get(int index1, int index2) {
		return this.data[index1+this.attributes.length*index2];
	}
	
	public double[] getData() {
		return this.data;
	}
	
	// total rows
	public int getSize() {
		return data.length / attributes.length;
	}
	
	// total data in bytes
	public int getDataSize() {
		return data.length * Double.SIZE / Byte.SIZE;
	}
}
