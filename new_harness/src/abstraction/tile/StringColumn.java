package abstraction.tile;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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
	
	// copied from TileDecoder class
	// first element is a double (8 bytes), the rest is a list of pairs:
	// (length of string (double, 8 bytes), the string (variable bytes))
	@Override
	public byte[] getBytes() {
		int numvals = columnVals.size();
		int[] lengths = new int[numvals];
		int sum = 0;
		for(int i = 0; i < numvals; i++) {
			lengths[i] = columnVals.get(i).length();
			sum += lengths[i];
		}
		byte[] result = new byte[(numvals + 1)*doubleSize + sum];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		buffer.putDouble(0,numvals); // how many bytes?
		int offset = doubleSize; // numvals is 8 bytes
		for(int i = 0; i < numvals; i++) {
			buffer.putDouble(offset,lengths[i]); // how long is the string?
			offset += doubleSize;
			byte[] stringBytes = columnVals.get(i).getBytes(defaultStringEncoding);
			for(int j = 0; j < stringBytes.length; j++,offset++) {
				buffer.put(offset,stringBytes[j]);
			}
		}
		return result;
	}
	
	// copied from TileDecoder class
	@Override
	public int readBytes(byte[] data, int offset) {
		this.columnVals.clear();
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int totalstrings = (int) buffer.getDouble(offset);
		//System.out.println("num strings: "+totalStrings+", offset: "+offset);
		offset += doubleSize;
		
		for(int i = 0; i < totalstrings; i++) {
			int strlen = (int) buffer.getDouble(offset);
			offset += doubleSize;
			//get the string
			byte[] stringdata = new byte[strlen];
			for(int j = 0; j < strlen; j++,offset++) {
				stringdata[j] = buffer.get(offset);
			}
			this.add(new String(stringdata,defaultStringEncoding));
			//System.out.println("attribute:"+result.get(i));
		}
		
		
		return offset;
	}
}
