package abstraction.tile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class FloatColumn extends Column {
	private static final long serialVersionUID = 2831079186367737982L;
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
