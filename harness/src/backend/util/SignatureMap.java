package backend.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SignatureMap implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4935333808629094022L;
	private Map<Model,Integer> sigTypes;
	private HashMap<String,List<double[]> > sigMap; // <id,histograms>
	
	// setup a new map from scratch
	public SignatureMap(Model[] sigTypes) {
		this.sigMap = new HashMap<String,List<double[]>>();
		this.sigTypes = new HashMap<Model,Integer>();
		for(int i = 0; i < sigTypes.length; i++) {
			this.sigTypes.put(sigTypes[i],i);
		}
	}
	
	public synchronized int size() {
		return this.sigMap.size();
	}
	
	public synchronized double[] getSignature(TileKey id, Model label) {
		double[] result = null;
		try {
			result = sigMap.get(id.buildTileStringForFile()).get(sigTypes.get(label));
		} catch (Exception e) {
			System.out.println("Could not retrieve signature for tile '"+id.buildTileStringForFile()+"'");
			e.printStackTrace();
		}
		return result;
	}
	
	// insert/update a given signature into the map
	public synchronized void updateSignature(TileKey id, Model label, double[] sig) {
		String key = id.buildTileStringForFile();
		List<double[]> newEntry = null;
		if(sigMap.containsKey(key)) {
			newEntry = this.sigMap.get(key);
		} else {
			newEntry = new ArrayList<double[]>();
			for(int i = 0; i < this.sigTypes.size(); i++) {
				newEntry.add(null);
			}
			this.sigMap.put(key, newEntry);
		}
		if(sig != null) {
			newEntry.set(this.sigTypes.get(label), Arrays.copyOf(sig,sig.length));
		}
	}
	
	
	// write this signature map to disk
	public synchronized boolean save(String filename) {
		File file = new File("sigMap.ser");
		if (filename != null) {
			file = new File(filename);
		}
		
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			out.writeObject(this);
			out.close();
			return true;
		} catch (FileNotFoundException e) {
			System.out.println("could not write Signaturemap to disk.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not write Signaturemap to disk.");
			e.printStackTrace();
		}
		return false;
	}
	
	// read a signature map from disk
	// static so it can be used without instantiating the class
	public static SignatureMap getFromFile(String filepath) {
		SignatureMap mp = null;
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filepath)));
	        mp = (SignatureMap) in.readObject();
	        in.close();
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+filepath);
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+filepath);
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+SignatureMap.class);
			e.printStackTrace();
		}
		return mp;
	}
}
