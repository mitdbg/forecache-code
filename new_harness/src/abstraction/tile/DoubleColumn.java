package abstraction.tile;

import java.util.ArrayList;
import java.util.List;

public class DoubleColumn extends Column {
	private static final long serialVersionUID = -6792254154438923457L;
	public List<Double> columnVals;
	public Domain<Double> domain;
	
	public DoubleColumn() {
		this.columnVals = new ArrayList<Double>();
		this.domain = new Domain<Double>();
	}
	
	@Override
	public List<Double> getValues() {
		return this.columnVals;
	}
	
	@Override
	public boolean isNumeric() {
		return true;
	}
	
	@Override
	public int getSize() {
		return this.columnVals.size();
	}
	
	@Override
	public List<Double> getDomain() {
		return this.domain.getDomain();
		
	}
	
	public void add(Double item) {
		this.columnVals.add(item);
		this.domain.update(item);
	}
	
	public void add(String value) {
		Double item = Double.parseDouble(value);
		add(item);
	}
	
	@Override
	public Object get(int i) {
		return this.columnVals.get(i);
	}
	
	public Double getTyped(int i) {
		return this.columnVals.get(i);
	}
	
	@Override
	public Class<Double> getColumnType() {
		return Double.class;
	}
}