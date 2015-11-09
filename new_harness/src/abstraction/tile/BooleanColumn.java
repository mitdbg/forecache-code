package abstraction.tile;

import java.util.ArrayList;
import java.util.List;


public class BooleanColumn extends Column {
	private static final long serialVersionUID = -4879283509057730872L;
	public List<Boolean> columnVals;
	public Domain<Boolean> domain;
	
	public BooleanColumn() {
		this.columnVals = new ArrayList<Boolean>();
		this.domain = new Domain<Boolean>();
	}
	
	@Override
	public List<Boolean> getValues() {
		return this.columnVals;
	}
	
	@Override
	public boolean isNumeric() {
		return false;
	}
	
	@Override
	public int getSize() {
		return this.columnVals.size();
	}
	
	@Override
	public List<Boolean> getDomain() {
		return this.domain.getDomain();
		
	}
	
	public void add(Boolean item) {
		this.columnVals.add(item);
		this.domain.update(item);
	}
	
	public void add(String value) {
		boolean item = Boolean.parseBoolean(value);
		add(item);
	}
	
	@Override
	public Object get(int i) {
		return this.columnVals.get(i);
	}
	
	public Boolean getTyped(int i) {
		return this.columnVals.get(i);
	}
	
	@Override
	public Class<Boolean> getColumnType() {
		return Boolean.class;
	}
}