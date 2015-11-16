package abstraction.tile;

import java.nio.ByteBuffer;
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
	
	@Override
	// each element is 8 bytes
	public byte[] getBytes() {
		int numvals = this.columnVals.size();
		byte[] result = new byte[(numvals+1)* doubleSize];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		buffer.putDouble(0,numvals); // how many bytes?
		int offset = doubleSize; // numvals is 8 bytes
		for(int i = 0; i < numvals; i++,offset+=doubleSize) {
			buffer.putDouble(offset,this.columnVals.get(i));
		}
		return result;
	}
}