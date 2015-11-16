package abstraction.tile;

import java.nio.ByteBuffer;
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
	
	// first element is a double, the rest are chars
	// each boolean value is represented as one byte
	// TODO: make each boolean value one bit
	@Override
	public byte[] getBytes() {
		int numvals = this.columnVals.size();
		byte[] result = new byte[numvals + doubleSize];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		buffer.putDouble(0,numvals); // how many bytes?
		int offset = doubleSize; // numvals is eight bytes
		for(int i = 0; i < numvals; i++,offset++) {
			buffer.putChar(offset,this.columnVals.get(i) ? '1' : '0');
		}
		return result;
	}
}