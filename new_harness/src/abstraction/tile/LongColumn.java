package abstraction.tile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class LongColumn extends Column {
	private static final long serialVersionUID = -7937808835949216174L;
	public List<Long> columnVals;
	public Domain<Long> domain;
	
	public LongColumn() {
		this.columnVals = new ArrayList<Long>();
		this.domain = new Domain<Long>();
	}
	
	@Override
	public List<Long> getValues() {
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
	public List<Long> getDomain() {
		return this.domain.getDomain();
		
	}
	
	public void add(Long item) {
		this.columnVals.add(item);
		this.domain.update(item);
	}
	
	public void add(String value) {
		Long item = Long.parseLong(value);
		add(item);
	}
	
	@Override
	public Object get(int i) {
		return this.columnVals.get(i);
	}
	
	public Long getTyped(int i) {
		return this.columnVals.get(i);
	}
	
	@Override
	public Class<Long> getColumnType() {
		return Long.class;
	}
	
	// first element is a double (8 bytes), the rest are longs (8 bytes)
	@Override
	public byte[] getBytes() {
		int numvals = this.columnVals.size();
		byte[] result = new byte[(numvals + 1)*doubleSize];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		buffer.putDouble(0,numvals); // how many bytes?
		int offset = doubleSize; // numvals is 8 bytes
		for(int i = 0; i < numvals; i++,offset+=doubleSize) {
			buffer.putLong(offset,this.columnVals.get(i));
		}
		return result;
	}
}