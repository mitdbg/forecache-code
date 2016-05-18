package abstraction.tile;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import abstraction.mdstructures.MultiDimTileKey;

public class MultiDimTileDecoder {
	public static int doubleSize = Column.doubleSize;
	public static Charset defaultStringEncoding = Column.defaultStringEncoding;
	
	// custom parser for turning tiles into bytes
	public static byte[] getBytes(MultiDimColumnBasedNiceTile tile) {
		int totalBytes = 0;
		byte[] keyBytes = packKey(tile);
		byte[] attributesBytes = packAttributes(tile);
		byte[] dataTypesBytes = packDataTypes(tile);
		totalBytes = attributesBytes.length+dataTypesBytes.length+keyBytes.length;
		List<byte[]> columnData = new ArrayList<byte[]>();
		for(Column c : tile.columns) {
			byte[] cBytes = c.getBytes();
			columnData.add(cBytes);
			totalBytes += cBytes.length;
		}
		int offset = 0;
		byte[] packedTile = new byte[totalBytes];
		System.arraycopy(keyBytes, 0, packedTile, offset, keyBytes.length);
		offset+=keyBytes.length;
		System.arraycopy(attributesBytes, 0, packedTile, offset, attributesBytes.length);
		offset += attributesBytes.length;
		System.arraycopy(dataTypesBytes, 0, packedTile, offset, dataTypesBytes.length);
		offset += dataTypesBytes.length;
		for(byte[] cBytes: columnData) {
			System.arraycopy(cBytes, 0, packedTile, offset, cBytes.length);
			offset += cBytes.length;	
		}
		return packedTile;
	}
	
	public static MultiDimColumnBasedNiceTile readBytes(byte[] data) {
		int offset = 0;
		MultiDimColumnBasedNiceTile tile = new MultiDimColumnBasedNiceTile();
		tile.id = new MultiDimTileKey(new int[]{},new int[]{});
		offset = unpackKey(data,offset,tile.id);
		System.out.println("current offset after key: "+offset);
		tile.attributes = new ArrayList<String>();
		tile.dataTypes = new ArrayList<Class<?>>();
		tile.columns = new ArrayList<Column>();
		offset = unpackAttributes(data,offset,tile.attributes);
		System.out.println("current offset after attributes: "+offset);
		offset = unpackDataTypes(data,offset,tile.dataTypes);
		System.out.println("current offset after data types: "+offset);
		int numcols = tile.dataTypes.size();
		for(int i = 0; i < numcols; i++) {
			Class<?> dType = tile.dataTypes.get(i);
			Column c = tile.getTypedColumn(dType);
			offset = c.readBytes(data, offset);
			System.out.println("current offset after column "+i+": "+offset);
			tile.columns.add(c);
		}
		return tile;
	}
	
	public MultiDimColumnBasedNiceTile getTile(byte[] encodedTile) {
		MultiDimColumnBasedNiceTile tile = new MultiDimColumnBasedNiceTile();
		return tile;
	}
	
 	// first element is a double (8 bytes), the rest is a list of pairs:
	// (length of string (double, 8 bytes), the string (variable bytes))
	public static byte[] packStrings(List<String> columnVals) {
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
	
	public static int unpackStrings(byte[] data, int offset, List<String> result) {
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
			result.add(new String(stringdata,defaultStringEncoding));
			//System.out.println("attribute:"+result.get(i));
		}
		
		return offset;
	}
	
	/***************** Helper Functions ********************/
	public static byte[] packKey(MultiDimColumnBasedNiceTile tile) {
		byte[] data = new byte[(tile.id.dimIndices.length+2)*doubleSize];
		ByteBuffer buffer = ByteBuffer.wrap(data);
		int offset = 0;
		//for zoom
		buffer.putDouble(offset,tile.id.zoom.length); // how long is the zoomPos?
		offset += doubleSize;
		for(int i = 0; i < tile.id.zoom.length; i++, offset+=doubleSize) {
			buffer.putDouble(offset,tile.id.zoom[i]);
		}
		//for dimIndices
		buffer.putDouble(offset,tile.id.dimIndices.length);
		offset += doubleSize;
		for(int i = 0; i < tile.id.dimIndices.length; i++, offset+=doubleSize) {
			buffer.putDouble(offset,tile.id.dimIndices[i]);
		}
		return data;
	}
	
	public static int unpackKey(byte[] data, int offset, MultiDimTileKey id) {
		ByteBuffer buffer = ByteBuffer.wrap(data);
		// for zoom
		int zoomLength = (int) buffer.getDouble(offset);
		offset += doubleSize;
		id.zoom = new int[zoomLength];
		for(int i = 0; i < zoomLength; i++,offset+=doubleSize) {
			id.zoom[i] = (int) buffer.getDouble(offset);
		}
		// for dimIndices
		int numvals = (int) buffer.getDouble(offset);
		offset += doubleSize;
		id.dimIndices = new int[numvals];
		for(int i = 0; i < numvals; i++,offset+=doubleSize) {
			id.dimIndices[i] = (int) buffer.getDouble(offset);
		}
		return offset;
	}
	
	public static byte[] packAttributes(MultiDimColumnBasedNiceTile tile) {
		return packStrings(tile.attributes);
	}
	
	public static int unpackAttributes(byte[] data, int offset, List<String> attributes) {
		return unpackStrings(data,offset, attributes);
	}
	
	public static byte[] packDataTypes(MultiDimColumnBasedNiceTile tile) {
		List<String> types = new ArrayList<String>();
		for(Class<?> type : tile.dataTypes) {
			types.add(type.getCanonicalName());
		}
		return packStrings(types);
	}
	
	public static int unpackDataTypes(byte[] data, int offset, List<Class<?>> dataTypes) {
		List<String> types = new ArrayList<String>();
		offset = unpackStrings(data,offset,types);
		for(String type : types) {
			dataTypes.add(getJavaType(type));
		}
		return offset;
	}
	
	public static Class<?> getJavaType(String type) {
		if(type.equals(Boolean.class.getCanonicalName())) return Boolean.class;
		else if (type.equals(String.class.getCanonicalName())) return String.class;
		else if(type.equals(Character.class.getCanonicalName())) return Character.class;
		else if (type.equals(Double.class.getCanonicalName())) return Double.class;
		else if (type.equals(Float.class.getCanonicalName())) return Float.class;
		else if (type.equals(Integer.class.getCanonicalName())) return Integer.class;
		else if (type.equals(Long.class.getCanonicalName())) return Long.class;
		else return String.class;
	}
	
	public static void main(String[] args) {
		List<String> attr = new ArrayList<String>();
		attr.add("str");
		attr.add("dbl");
		
		List<Class<?>> dataTypes = new ArrayList<Class<?>>();
		dataTypes.add(String.class);
		dataTypes.add(Double.class);
		List<String> data = new ArrayList<String>();
		data.add("hello");
		data.add("-2.0");
		data.add("there");
		data.add("3.5");
		data.add("it");
		data.add("0");
		data.add("works!");
		data.add("100.999995");
		
		MultiDimColumnBasedNiceTile t = new MultiDimColumnBasedNiceTile();
		t.initializeData(data, dataTypes, attr);
		t.id = new MultiDimTileKey(new int[]{0,0},new int[]{0});
		
		System.out.println("testing tile encoding/decoding");
		byte[] encodedTile = MultiDimTileDecoder.getBytes(t);
		MultiDimColumnBasedNiceTile t2 = MultiDimTileDecoder.readBytes(encodedTile);
		int dbl_index = t2.getIndex("dbl");
		int count = t2.getSize();
		System.out.println("total rows: "+count);
		List<?> domain = t2.getDomain(dbl_index);
		System.out.println(t2.attributes.get(dbl_index)+" domain: ["+domain.get(0)+","+domain.get(1)+"]");
		for(int i = 0; i < count; i++) {
			System.out.println(t2.attributes.get(0)+": "+t2.get(0,i));
			System.out.println(t2.attributes.get(1)+": "+t2.get(1,i));
		}
	}
}
