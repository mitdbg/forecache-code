package backend.disk;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import com.google.gson.JsonArray;

import utils.DBInterface;
import utils.UtilityFunctions;
import backend.util.NiceTile;
import backend.util.TileKey;

public class NiceTilePacker {
	private static double totalTime = 0;// in milliseconds
	public static int doubleSize = 8;
	protected static String customBinarySuffix = "_binary";
	protected int cache_size = 1;
	
	public static boolean writeNiceTile(NiceTile tile) {
		return writeNiceTileDefault(tile); // default Java serialization
		//return writeNiceTileFST(tile); // serialization via FST
		//return writeNiceTileCustom(tile); // serialization via FST
	}
	
	public static NiceTile readNiceTile(TileKey id) {
			return readNiceTileDefault(id); // default Java deserialization
			//return = readNiceTileFST(id); // deserialization via FST
			//return = readNiceTileCustom(id); // serialization via FST
	}
	
	public static String makeJson(NiceTile tile) {
		/*
		public TileKey id;
		public double[] data;
		public double[][] extrema = null;
		public String[] attributes = null;
		public double[] norm = null;
		public double[] hist = null;
		public double[] fhist = null;
		*/
		String id = tile.id.buildTileStringForFile();
		StringBuilder sb = new StringBuilder();
		sb.append("{").append("\"id\":{")
		.append("\"zoom\":").append(tile.id.zoom).append(",\"coords\":[").append(tile.id.id[0]);
		for(int i = 1; i < tile.id.id.length; i++) {
			sb.append(",").append(tile.id.id[i]);
		}
		sb.append("]},");
		sb.append("\"data\":[");
		if(tile.data.length > 0) {
			sb.append(tile.data[0]);
		}
		for(int i = 1; i < tile.data.length; i++) {
			sb.append(",").append(tile.data[i]);
		}
		sb.append("],");
		
		sb.append("\"attributes\":[");
		if(tile.attributes.length > 0) {
			sb.append("\"").append(tile.attributes[0]).append("\"");
		}
		for(int i = 1; i < tile.attributes.length; i++) {
			sb.append(",").append("\"").append(tile.attributes[i]).append("\"");
		}
		sb.append("],");
		
		sb.append("\"extrema\":{");
		if(tile.extrema.length > 0 && tile.extrema[0].length > 0) {
			sb.append("\"min\":[");
			sb.append(tile.extrema[0][0]);
			for(int i = 1; i < tile.extrema.length; i++) {
				sb.append(",").append(tile.extrema[i][0]);
			}
			sb.append("],\"max\":[");
			sb.append(tile.extrema[0][1]);
			for(int i = 1; i < tile.extrema.length; i++) {
				sb.append(",").append(tile.extrema[i][1]);
			}
			sb.append("]");
		}
		sb.append("}");
		
		sb.append("}");
		return sb.toString();
	}
	
	/******* Custom Tile Serialization ******/
	public static byte[] packNiceTile(NiceTile t) {
		//long start = System.currentTimeMillis();
		byte[] tileKey = packTileKey(t.id);
		byte[] attributes = packAttributes(t.attributes);
		byte[] extrema = packExtrema(t.extrema);
		byte[] data = packData(t.data);
		byte[] packedTile = new byte[tileKey.length+attributes.length+extrema.length+data.length];
		int offset = 0;
		System.arraycopy(tileKey, 0, packedTile, offset, tileKey.length);
		offset+=tileKey.length;
		System.arraycopy(attributes,0,packedTile,offset,attributes.length);
		offset+=attributes.length;
		System.arraycopy(extrema, 0, packedTile, offset, extrema.length);
		offset +=extrema.length;
		System.arraycopy(data,0,packedTile,offset,data.length);
		return packedTile;
	}
	
	//[row1col1,row1col2,row1col3,...,row2col1,row2col2,row3col3,...,row3col1,...]
	public static byte[] packData(double[] data) {
		//long start = System.currentTimeMillis();
		byte [] result = new byte[doubleSize*data.length];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		for(int i = 0; i < data.length; i++) {
			buffer.putDouble(i*doubleSize, data[i]);
		}
		//long end = System.currentTimeMillis();
		//totalTime += 1.0 * (end - start);
		//System.out.println("Time to convert to bytes: "+(end-start)+"ms");
		return result;
	}
	
	//[dimcount,dim1val,dim2val,dim3val,...,weightscount,dim1weight,dim2weight,dim3weight,...,zoomval]
	public static byte[] packTileKey(TileKey key) {
		//accounts for id, weights, and zoom
		//long start = System.currentTimeMillis();
		byte[] result = new byte[doubleSize*(1+key.id.length+1+key.weights.length+1)];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		int offset = 0;
		buffer.putDouble(offset,key.id.length);
		offset+= doubleSize;
		for(int count = 0; count < key.id.length;count++,offset += doubleSize) {
			buffer.putDouble(offset,key.id[count]);
		}
		buffer.putDouble(offset,key.weights.length);
		offset += doubleSize;
		for(int count = 0; count < key.weights.length;count++,offset += doubleSize) {
			buffer.putDouble(offset,key.weights[count]);
		}
		buffer.putDouble(offset,key.zoom);
		//long end = System.currentTimeMillis();
		//totalTime += 1.0*(end- start);
		return result;
	}
	
	//[attrcount,attr1min,att1max,attr2min,attr2max,...]
	public static byte[] packExtrema(double[][]extrema) {
		//long start = System.currentTimeMillis();
		byte[] result = new byte[doubleSize*(1+extrema.length*2)];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		int i = 0;
		buffer.putDouble(i,extrema.length);
		i++;
		for(int count = 0; count< extrema.length;count++,i+=2) {
			buffer.putDouble(i*doubleSize,extrema[count][0]);
			buffer.putDouble((i+1)*doubleSize,extrema[count][1]);
			//System.out.println("recording min: "+extrema[count][0]+", max: "+extrema[count][1]);
		}
		//System.out.println();
		//long end = System.currentTimeMillis();
		//totalTime += 1.0*(end- start);
		return result;
	}
	
	//[attrcount,label1len,label2len,label3len,...,label1,label2,label3,...]
	public static byte[] packAttributes(String[] attributes) {
		//long start = System.currentTimeMillis();
		StringBuilder builder = new StringBuilder();
		int[] lengths = new int[attributes.length];
		for(int i = 0; i < attributes.length; i++) {
			builder.append(attributes[i]);
			lengths[i] = attributes[i].length();
		}
		try {
			String finalString = builder.toString();
			byte[] finalBytes = finalString.getBytes("ISO-8859-1");
			byte[] result = new byte[doubleSize*(attributes.length+1)+finalBytes.length];
			ByteBuffer buffer = ByteBuffer.wrap(result);
			int offset = 0;
			buffer.putDouble(offset,attributes.length);
			offset+=doubleSize;
			for(int i = 0; i < attributes.length; i++) {
				buffer.putDouble(offset,lengths[i]);
				offset += doubleSize;
			}
			for(int i = 0; i < finalBytes.length; i++) {
				buffer.put(offset,finalBytes[i]);
				offset++;
			}
			return result;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new byte[0];
	}
	
	public static boolean writeNiceTileCustom(NiceTile tile) {
		long start = System.currentTimeMillis();
		// pack id -> pack id, weights, zoom
		byte[] key = packTileKey(tile.id);
		// pack attributes
		byte[] attributes = packAttributes(tile.attributes);
		// pack extrema
		byte[] extrema = packExtrema(tile.extrema);
		// pack data
		byte[] data = packData(tile.getData());
		
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		TileKey id = tile.id;
		File file = new File(dir,id.buildTileStringForFile()+customBinarySuffix);

		try {
			FileOutputStream out = new FileOutputStream(file);
			out.write(key);
			out.write(attributes);
			out.write(extrema);
			out.write(data);
			out.close();
			long end = System.currentTimeMillis();
			totalTime += 1.0*(end - start);
			return true;
		} catch (FileNotFoundException e) {
			System.out.println("could not write NiceTile to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write NiceTile to disk.");
			e.printStackTrace();
		}
		return false;
	}
	
	public static int unpackTileKey(byte[] rawdata, int offset, NiceTile t) {
		//long start = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.wrap(rawdata);
		int[] id = new int[(int) buffer.getDouble(offset)];
		//System.out.println("id len: "+buffer.getDouble(offset)+", offset: "+offset);
		offset += doubleSize;
		for(int i = 0; i < id.length; i++, offset += doubleSize) {
			id[i] = (int) buffer.getDouble(offset);
			//System.out.println("dim: "+id[i]+", offset: "+offset);
		}
		double[] weights = new double[(int)buffer.getDouble(offset)];
		//System.out.println("weights len: "+weights.length);
		offset += doubleSize;
		for(int i = 0; i < weights.length; i++,offset += doubleSize) {
			weights[i] = buffer.getDouble(offset);
			//System.out.println("weight: "+weights[i]+", offset: "+offset);
		}
		int zoom = (int) buffer.getDouble(offset);
		//System.out.println("zoom: "+buffer.getDouble(offset)+", offset: "+offset);
		offset += doubleSize; // the next item will be one double away
		TileKey key = new TileKey(id,zoom);
		key.weights = weights;
		t.id = key; // set the new key for this tile
		//long end = System.currentTimeMillis(); // record how long this took
		//totalTime += 1.0*(end- start);
		return offset; // return the new offset
	}
	
	//[attrcount,label1len,label2len,label3len,...,label1,label2,label3,...]
	public static int unpackAttributes(byte[] rawdata, int offset, NiceTile t) {
		//long start = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.wrap(rawdata);
		//System.out.println("num attrs: "+buffer.getDouble(offset)+", offset: "+offset);
		String[] attributes = new String[(int) buffer.getDouble(offset)];
		offset += doubleSize;
		int[] attrlens = new int[attributes.length];
		for(int i = 0; i < attributes.length; i++, offset+=doubleSize) {
			//get string length in bytes
			attrlens[i] = (int) buffer.getDouble(offset);
			//System.out.println("attr size: "+buffer.getDouble(offset)+", offset: "+offset);
		}
		for(int i = 0; i < attributes.length; i++) {
			byte[] attrdata = new byte[attrlens[i]];
			//get the string
			for(int j = 0; j < attrdata.length; j++) {
				attrdata[j] = buffer.get(offset);
				offset++;
			}
			try {
				attributes[i] = new String(attrdata,"utf-8");
				//System.out.println("attribute:"+attributes[i]);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		t.attributes = attributes;
		//long end = System.currentTimeMillis(); // record how long this took
		//totalTime += 1.0*(end- start);
		return offset;
	}
	
	public static int unpackExtrema(byte[] rawdata, int offset, NiceTile t) {
		//long start = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.wrap(rawdata);
		//System.out.println("size: "+buffer.getDouble(offset));
		double[][] extrema = new double [(int) buffer.getDouble(offset)][];
		offset += doubleSize;
		//System.out.println("extrema length: "+extrema.length);
		for(int i = 0; i < extrema.length; i++,offset+=doubleSize*2) {
			extrema[i] = new double[2];
			extrema[i][0] = buffer.getDouble(offset);
			extrema[i][1] = buffer.getDouble(offset+doubleSize);
			//System.out.println("min: "+extrema[i][0]+", max: "+extrema[i][1]);
		}
		t.extrema = extrema;
		//long end = System.currentTimeMillis();
		//totalTime += 1.0*(end- start);
		return offset;
	}
	
	public static int unpackData(byte[] rawdata, int offset, NiceTile t) {
		//long start = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.wrap(rawdata);
		int count = (rawdata.length - offset + 1) / doubleSize;
		//System.out.println("count: "+count);
		if(count < 0) count = 0;
		double[] result = new double[count];
		int i = 0;
		//System.out.println("total points: "+count);
		for(; i < count; i++) {
			result[i] = buffer.getDouble(i*doubleSize+offset);
			//if(i < 12) {
			//	System.out.println("data: "+result[i]);
			//}
		}
		t.data = result;
		//long end = System.currentTimeMillis();
		//totalTime += 1.0*(end-start);
		//System.out.println("Time to convert from bytes to doubles: "+(end-start)+"ms");
		return i*doubleSize+offset;
	}
	
	public static NiceTile unpackNiceTile(byte[] rawdata)  {
		//long start = System.currentTimeMillis();
		NiceTile result = new NiceTile();
		int offset = 0;
		// unpack TileKey
		offset = unpackTileKey(rawdata, offset, result);
		// unpack attributes
		offset = unpackAttributes(rawdata, offset, result);
		// unpack extrema
		offset = unpackExtrema(rawdata, offset, result);
		// unpack data
		unpackData(rawdata, offset, result);
		//long end = System.currentTimeMillis();
		//totalTime += 1.0*(end-start);
		return result;
	}
	
	public static NiceTile readNiceTileCustom(TileKey id) {
		long start = System.currentTimeMillis();
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+customBinarySuffix);
		NiceTile tile = null;
		try {
	        byte[] rawdata = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
			tile = unpackNiceTile(rawdata);
			long end = System.currentTimeMillis();
			totalTime += 1.0*(end - start);
	        return tile;
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+file);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+file);
			//e.printStackTrace();
		}
		return null;
	}
	
	
	/******* FST Java Serialization ******/
	public static boolean writeNiceTileFST(NiceTile tile) {
		long start = System.currentTimeMillis();
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		TileKey id = tile.id;
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		FSTObjectOutput out;
		try {
			out = new FSTObjectOutput(new FileOutputStream(file));
			out.writeObject(tile);
			out.close();
			long end = System.currentTimeMillis();
			totalTime += 1.0*(end - start);
			return true;
		} catch (FileNotFoundException e) {
			System.out.println("could not write NiceTile to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write NiceTile to disk.");
			e.printStackTrace();
		}
		return false;
	}
	
	public static NiceTile readNiceTileFST(TileKey id) {
		long start = System.currentTimeMillis();
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		
		FSTObjectInput in;
		try {
			in = new FSTObjectInput(new FileInputStream(file));
	        NiceTile tile = (NiceTile) in.readObject();
	        in.close();
			long end = System.currentTimeMillis();
			totalTime += 1.0*(end - start);
	        return tile;
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+file);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+file);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+NiceTile.class);
			e.printStackTrace();
		}
		return null;
	}
	
	/******* Default Java Serialization ******/
	public static boolean removeNiceTile(TileKey id) {
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		return file.delete();
	}
	
	public static NiceTile readNiceTileDefault(File file) {
		NiceTile t = null;
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
	        t = (NiceTile) in.readObject();
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+file);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+file);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+NiceTile.class);
			e.printStackTrace();
		}
		return t;
	}
	
	public static boolean writeNiceTileDefault(NiceTile tile) {
		long start = System.currentTimeMillis();
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		TileKey id = tile.id;
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			out.writeObject(tile);
			out.close();
			long end = System.currentTimeMillis();
			totalTime += 1.0*(end - start);
			return true;
		} catch (FileNotFoundException e) {
			System.out.println("could not write NiceTile to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write NiceTile to disk.");
			e.printStackTrace();
		}
		return false;
	}
	
	public static NiceTile readNiceTileDefault(TileKey id) {
		long start = System.currentTimeMillis();
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		NiceTile tile = readNiceTileDefault(file);
		long end = System.currentTimeMillis();
		totalTime += 1.0*(end - start);
		return tile;
	}
	
	public static double getTotalTime() {
		return totalTime;
	}
	
	public static void main(String[] args) {
		boolean def = true;
		boolean fst = false;
		boolean custom = false;
		boolean baseline = false;
		OldScidbTileInterface sti = new OldScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		String idstr = "[0, 0]";
		int zoom = 0;
		int[] tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		TileKey id = new TileKey(tile_id,zoom);
		NiceTile t = sti.getNiceTile(id);
		
		int iterations = 1000;
		
		if(baseline) {
			try {
				System.out.println("baseline to beat");
				DiskTileBuffer dbuf = new DiskTileBuffer(DBInterface.cache_root_dir,DBInterface.hashed_query,DBInterface.threshold);
				double time = 0.0;
				for(int i = 0; i < iterations; i++) {
					long start = System.currentTimeMillis();
					dbuf.getTile(id);
					long end = System.currentTimeMillis();
					time += 1.0*(end - start);
				}
				time /= iterations;
				System.out.println("total time to read: "+time+"ms");
			} catch (Exception e) {
				System.out.println("could not retrieve tile from disk buffer");
				e.printStackTrace();
			}
			sti.getNiceTile(id);
		}
		
		if(def) {
			System.out.println("default");
			double time = 0.0;
			for(int i = 0; i < iterations; i++) {
				totalTime = 0;
				writeNiceTileDefault(t);
				time += totalTime;
			}
			time /= iterations;
			System.out.println("total time to write: "+time+"ms");
			time = 0.0;
			for(int i = 0; i < iterations; i++) {
				totalTime = 0;
				readNiceTileDefault(id);
				time += totalTime;
			}
			time /= iterations;
			System.out.println("total time to read: "+time+"ms");
		}
		
		if(fst) {
			System.out.println("fst");
			double time = 0.0;
			for(int i = 0; i < iterations; i++) {
				totalTime = 0;
				writeNiceTileFST(t);
				time += totalTime;
			}
			time /= iterations;
			System.out.println("total time to write: "+time+"ms");
			time = 0.0;
			for(int i = 0; i < iterations; i++) {
				totalTime = 0;
				readNiceTileFST(id);
				time += totalTime;
			}
			time /= iterations;
			System.out.println("total time to read: "+time+"ms");
		}
		
		if(custom) {
			System.out.println("custom");
			double time = 0.0;
			for(int i = 0; i < iterations; i++) {
				totalTime = 0;
				writeNiceTileCustom(t);
				time += totalTime;
			}
			time /= iterations;
			System.out.println("total time to write: "+time+"ms");
			time = 0.0;
			for(int i = 0; i < iterations; i++) {
				totalTime = 0;
				readNiceTileCustom(id);
				time += totalTime;
			}
			time /= iterations;
			System.out.println("total time to read: "+time+"ms");
		}
	}
}
