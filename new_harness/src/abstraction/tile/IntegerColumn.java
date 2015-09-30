package abstraction.tile;

import java.util.ArrayList;
import java.util.List;

public class IntegerColumn extends Column {
	public List<Integer> columnVals;
	public Domain<Integer> domain;
	
	public IntegerColumn() {
		this.columnVals = new ArrayList<Integer>();
		this.domain = new Domain<Integer>();
	}
	
	@Override
	public List<Integer> getValues() {
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
	public List<Integer> getDomain() {
		return this.domain.getDomain();
		
	}
	
	public void add(Integer item) {
		this.columnVals.add(item);
		this.domain.update(item);
	}
	
	public void add(String value) {
		Integer item = Integer.parseInt(value);
		add(item);
	}
	
	public Integer get(int i) {
		return this.columnVals.get(i);
	}
	
	@Override
	public Class<Integer> getColumnType() {
		return Integer.class;
	}
}
