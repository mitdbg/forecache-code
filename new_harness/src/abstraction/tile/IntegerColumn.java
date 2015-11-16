package abstraction.tile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class IntegerColumn extends Column {
	private static final long serialVersionUID = 1440848013736117720L;
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
	
	@Override
	// each element is a double (8 bytes)
	public byte[] getBytes() {
		int numvals = this.columnVals.size();
		byte[] result = new byte[(numvals + 1)*doubleSize];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		buffer.putDouble(0,numvals); // how many bytes?
		int offset = doubleSize; // numvals is 8 bytes
		for(int i = 0; i < numvals; i++,offset+=doubleSize) {
			buffer.putDouble(offset,this.columnVals.get(i));
		}
		return result;
	}
}
