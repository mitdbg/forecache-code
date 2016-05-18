package abstraction.mdstorage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import abstraction.mdstructures.MultiDimTileKey;
import abstraction.tile.MultiDimColumnBasedNiceTile;
import abstraction.util.DBInterface;

public class MultiDimTilePacker {
	private static double totalTime = 0;// in milliseconds
	public static int doubleSize = 8;
	protected static String customBinarySuffix = "_binary";
	protected int cache_size = 1;
	
	public static byte[] serializeTile(MultiDimColumnBasedNiceTile tile) {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try {
			ObjectOutput out = new ObjectOutputStream(byteStream);
			out.writeObject(tile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return byteStream.toByteArray();
	}
	
	public static MultiDimColumnBasedNiceTile deserializeTile(byte[] byteArray) {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
		ObjectInput in;
		MultiDimColumnBasedNiceTile tile = null;
		try {
			in = new ObjectInputStream(inputStream);
			tile = (MultiDimColumnBasedNiceTile) in.readObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tile;
	}
	
	public static boolean writeNiceTile(MultiDimColumnBasedNiceTile tile) {
		return writeNiceTileDefault(tile); // default Java serialization
	}
	
	public static MultiDimColumnBasedNiceTile readNiceTile(MultiDimTileKey id) {
			return readNiceTileDefault(id); // default Java deserialization
	}
	
	/******* Default Java Serialization ******/
	public static boolean removeNiceTile(MultiDimTileKey id) {
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		return file.delete();
	}
	
	/*
	public static MultiDimColumnBasedNiceTile readNiceTile(File file) {
		MultiDimColumnBasedNiceTile tile = null;
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
	        tile = (MultiDimColumnBasedNiceTile) in.readObject();
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+file);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+file);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+MultiDimColumnBasedNiceTile.class);
			e.printStackTrace();
		}
		return tile;
	}
	*/
	
	public static boolean writeNiceTileDefault(MultiDimColumnBasedNiceTile tile) {
		return writeNiceTile(tile,DBInterface.nice_tile_cache_dir);
	}
	
	public static MultiDimColumnBasedNiceTile readNiceTileDefault(MultiDimTileKey id) {
		return readNiceTile(id,DBInterface.nice_tile_cache_dir);
	}
	
	public static boolean writeNiceTile(MultiDimColumnBasedNiceTile tile, String dirpath) {
		long start = System.currentTimeMillis();
		File dir = new File(dirpath);
		dir.mkdirs();
		MultiDimTileKey id = tile.id;
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
			System.out.println("could not write MultiDimColumnBasedNiceTile to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write MultiDimColumnBasedNiceTile to disk.");
			e.printStackTrace();
		}
		return false;
	}
	
	public static MultiDimColumnBasedNiceTile readNiceTile(MultiDimTileKey id, String dirpath) {
		long start = System.currentTimeMillis();
		File dir = new File(dirpath);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		MultiDimColumnBasedNiceTile tile = null; //readNiceTile(file);
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
	        tile = (MultiDimColumnBasedNiceTile) in.readObject();
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+file);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+file);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+MultiDimColumnBasedNiceTile.class);
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		totalTime += 1.0*(end - start);
		return tile;
	}
	
	public static double getTotalTime() {
		return totalTime;
	}
}
