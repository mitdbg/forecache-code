package abstraction.tile;

import java.util.ArrayList;
import java.util.List;

public class StringColumn extends Column {
	private static final long serialVersionUID = -3614065250292923396L;
	public List<String> columnVals;
	public Domain<String> domain = null;
	
	public StringColumn() {
		this.columnVals = new ArrayList<String>();
		this.domain = new Domain<String>();
	}
	
	@Override
	public List<String> getValues() {
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
	public List<String> getDomain() {
		return this.domain.getDomain();
		
	}
	
	public void add(String item) {
		this.columnVals.add(item);
		//don't actually calculate the domain
		//this.domain.update(item);
	}
	
	@Override
	public Object get(int i) {
		return this.columnVals.get(i);
	}
	
	public String getTyped(int i) {
		return this.columnVals.get(i);
	}
	
	@Override
	public Class<String> getColumnType() {
		return String.class;
	}
}
