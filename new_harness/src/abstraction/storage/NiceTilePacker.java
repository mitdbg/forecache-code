package abstraction.storage;

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

import abstraction.structures.NewTileKey;
import abstraction.tile.ColumnBasedNiceTile;
import abstraction.util.DBInterface;

public class NiceTilePacker {
	private static double totalTime = 0;// in milliseconds
	public static int doubleSize = 8;
	protected static String customBinarySuffix = "_binary";
	protected int cache_size = 1;
	
	public static byte[] serializeTile(ColumnBasedNiceTile tile) {
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
	
	public static ColumnBasedNiceTile deserializeTile(byte[] byteArray) {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
		ObjectInput in;
		ColumnBasedNiceTile tile = null;
		try {
			in = new ObjectInputStream(inputStream);
			tile = (ColumnBasedNiceTile) in.readObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tile;
	}
	
	public static boolean writeNiceTile(ColumnBasedNiceTile tile) {
		return writeNiceTileDefault(tile); // default Java serialization
	}
	
	public static ColumnBasedNiceTile readNiceTile(NewTileKey id) {
			return readNiceTileDefault(id); // default Java deserialization
	}
	
	/******* Default Java Serialization ******/
	public static boolean removeNiceTile(NewTileKey id) {
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		return file.delete();
	}
	
	/*
	public static ColumnBasedNiceTile readNiceTile(File file) {
		ColumnBasedNiceTile tile = null;
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
	        tile = (ColumnBasedNiceTile) in.readObject();
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+file);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+file);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+ColumnBasedNiceTile.class);
			e.printStackTrace();
		}
		return tile;
	}
	*/
	
	public static boolean writeNiceTileDefault(ColumnBasedNiceTile tile) {
		return writeNiceTile(tile,DBInterface.nice_tile_cache_dir);
	}
	
	public static ColumnBasedNiceTile readNiceTileDefault(NewTileKey id) {
		return readNiceTile(id,DBInterface.nice_tile_cache_dir);
	}
	
	public static boolean writeNiceTile(ColumnBasedNiceTile tile, String dirpath) {
		long start = System.currentTimeMillis();
		File dir = new File(dirpath);
		dir.mkdirs();
		NewTileKey id = tile.id;
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
			System.out.println("could not write ColumnBasedNiceTile to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write ColumnBasedNiceTile to disk.");
			e.printStackTrace();
		}
		return false;
	}
	
	public static ColumnBasedNiceTile readNiceTile(NewTileKey id, String dirpath) {
		long start = System.currentTimeMillis();
		File dir = new File(dirpath);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		ColumnBasedNiceTile tile = null; //readNiceTile(file);
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
	        tile = (ColumnBasedNiceTile) in.readObject();
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+file);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+file);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+ColumnBasedNiceTile.class);
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
