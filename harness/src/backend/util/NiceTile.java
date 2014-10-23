package backend.util;

import java.util.ArrayList;
import java.util.List;

public class NiceTile {
	public TileKey id;
	public List<List<Double>> data;
	public List<MinMax> extrema;
	public List<String> attributes;
	private int size = 0;
	
	public NiceTile() {
		this.data = new ArrayList<List<Double>>();
		this.attributes = new ArrayList<String>();
		this.extrema = new ArrayList<MinMax>();
		this.id = null;
	}
	
	public NiceTile(TileKey id) {
		this();
		this.id = id;
	}
	
	public void addAttribute(String attr) {
		this.attributes.add(attr);
		extrema.add(new MinMax());
		this.data.add(new ArrayList<Double>());
	}
	
	public int getIndex(String name) {
		return this.attributes.indexOf(name);
	}
	
	public void insert(Double val, int index) {
		this.data.get(index).add(val);
		
		// track max and min values
		MinMax mm = extrema.get(index);
		if(val != null) {
			if(mm.min == null || val < mm.min) {
				mm.min = val;
			}
			if(mm.max == null || val > mm.max) {
				mm.max = val;
			}
		}
	}
	
	public List<Double> getColumn(String name) {
		int index = attributes.indexOf(name);
		if(index >= 0) {
			return data.get(index);
		}
		return null;
	}
	
	public int getDataSize() {
		if(data.size() == 0) {
			size = 0;
		} else {
			size = attributes.size() * data.get(0).size() * Double.SIZE / Byte.SIZE;
		}
		return size;
	}
	
	public class MinMax {
		public Double min = null;
		public Double max = null;
	}
}
