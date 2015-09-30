package abstraction.tile;

import java.util.ArrayList;
import java.util.List;

public class FloatColumn extends Column {
	public List<Float> columnVals;
	public Domain<Float> domain;
	
	public FloatColumn() {
		this.columnVals = new ArrayList<Float>();
		domain = new Domain<Float>();
	}
	
	@Override
	public List<Float> getValues() {
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
	public List<?> getDomain() {
		return this.domain.getDomain();
		
	}
	
	public void add(Float item) {
		this.columnVals.add(item);
		this.domain.update(item);
	}
	
	@Override
	public void add(String value) {
		Float item = Float.parseFloat(value);
		add(item);
	}
	
	@Override
	public Object get(int i) {
		return this.columnVals.get(i);
	}
	
	public Float getTyped(int i) {
		return this.columnVals.get(i);
	}
	
	@Override
	public Class<?> getColumnType() {
		return Float.class;
	}
}
