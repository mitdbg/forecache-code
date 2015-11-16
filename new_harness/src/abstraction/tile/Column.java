package abstraction.tile;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class Column implements java.io.Serializable {
	private static final long serialVersionUID = -5580879891349835855L;
	public static int doubleSize = 8;
	public static String defaultStringEncoding = "ISO-8859-1";
	public abstract void add(String string);
	public abstract Object get(int i);
	public abstract Class<?> getColumnType();
	public abstract List<?> getDomain();
	public abstract int getSize();
	public abstract List<?> getValues();
	public abstract boolean isNumeric();
	public abstract byte[] getBytes();
	
 	// first element is a double (8 bytes), the rest is a list of pairs:
	// (length of string (double, 8 bytes), the string (variable bytes))
	public static byte[] packStrings(List<String> columnVals) {
		byte[] result = null;
		try {
			int numvals = columnVals.size();
			int[] lengths = new int[numvals];
			int sum = 0;
			for(int i = 0; i < numvals; i++) {
				lengths[i] = columnVals.get(i).length();
				sum += lengths[i];
			}
			result = new byte[(numvals + 1)*doubleSize + sum];
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
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
