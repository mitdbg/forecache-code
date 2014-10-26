package backend.disk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import utils.DBInterface;
import utils.UtilityFunctions;

import backend.util.NiceTile;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.Tile;
import backend.util.TileKey;

/*
 * calls SciDB from command line
 */
public class ScidbTileInterface {
	private ParamsMap paramsMap;
	private String paramsfile;
	private String delim;
	
	public ScidbTileInterface() {
		this(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}
	
	public ScidbTileInterface(String paramsfile, String delim) {
		this.paramsfile = paramsfile;
		this.delim = delim;
		this.paramsMap = new ParamsMap(this.paramsfile,this.delim);
	}
	
	// inserts parameters into aggregation query
	public String buildQuery(String arrayname,Params p) {
		String query = "regrid(subarray("+arrayname+","+p.xmin+","+p.ymin+","+p.xmax+","+p.ymax+"),"+p.width+","+p.width+
				",avg(ndsi) as avg_ndsi,min(ndsi) as min_ndsi,max(ndsi) as max_ndsi,max(land_sea_mask) as max_land_sea_mask)";
		//System.out.println("query: "+query);
		return query;
	}
	
	public String[] buildCmd(String arrayname, Params p) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER=12.10 ; " +
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; " +
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; " +
				"source ~/.bashrc ; iquery -o csv+ -aq \"" + buildQuery(arrayname,p) + "\"";
		return myresult;
	}
	
	// uses default array name
	public List<Double> executeQuery(Params p) {
		List<Double> myresult = new ArrayList<Double>();
		try {
			myresult = executeQuery(DBInterface.arrayname,p);
		} catch(IOException e) {
			System.out.println("Error occured while retrieving tile from database");
			e.printStackTrace();
		}
		return myresult;
	}
	
	public boolean writeNiceTile(NiceTile tile) {
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		TileKey id = tile.id;
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		ObjectOutputStream out;
		try {
			out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(tile);
			out.close();
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
	
	public NiceTile readNiceTile(TileKey id) {
		File dir = new File(DBInterface.nice_tile_cache_dir);
		dir.mkdirs();
		File file = new File(dir,id.buildTileStringForFile()+".ser");
		
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new FileInputStream(file));
	        NiceTile tile = (NiceTile) in.readObject();
	        in.close();
	        return tile;
		} catch (FileNotFoundException e) {
			System.out.println("could not find file: "+file);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("could not read file from disk: "+file);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("could not find class definition for class: "+NiceTile.class);
			e.printStackTrace();
		}
		return null;
	}
	
	public NiceTile getNiceTile(TileKey id) {
		NiceTile tile = readNiceTile(id);
		if(tile != null) return tile;
		
		tile = new NiceTile(id);
		String tile_id = id.buildTileString();
		if(tile_id == null) {
			System.out.println("could not build tile_id");
			return null;
		}
		Map<Integer,Params> map1 = this.paramsMap.get(id.buildTileString());
		if(map1 == null) {
			System.out.println("map1 is null");
			return null;
		}
		Params p = map1.get(id.zoom);
		if(p == null) {
			System.out.println("params is null");
			return null;
		}
		try {
			executeQuery(DBInterface.arrayname,p,tile);
		} catch(IOException e) {
			System.out.println("Error occured while retrieving tile from database");
			e.printStackTrace();
		}
		writeNiceTile(tile);
		return tile;
	}
	
	public void executeQuery(String arrayname, Params p, NiceTile tile) throws IOException {
		String[] cmd = buildCmd(arrayname, p);
		
		// print command
		//UtilityFunctions.printStringArray(cmd);
		//System.out.println();
		
		Process proc = Runtime.getRuntime().exec(cmd);
		/*
		// only uncomment this if things aren't working
		BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		for (String line; (line = ebr.readLine()) != null;) {
			System.out.println(line);
		}
		*/
		long start = System.currentTimeMillis();
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        boolean first = true;
		for (String line; (line = br.readLine()) != null;) {
			//System.out.println(line);
			String[] tokens = line.split(",");
			if(!first) { // ignore first line
				for(int i = 0; i < tokens.length; i++) {
					Double next = null;
					try {
						next = Double.parseDouble(tokens[i]);
					} catch (NumberFormatException e) {
						System.out.println("could not parse SciDB result, skipping...");
						e.printStackTrace();
					}
					tile.insert(next,i); // just add null if we can't parse it
				}
			} else {
				first = false;
				for(int i = 0; i < tokens.length; i++) {
					tile.addAttribute(tokens[i]);
				}
			}
		}
		/*
		for(int i = 0; i < tile.attributes.size(); i++) {
			System.out.print(tile.attributes.get(i)+"\t");
			System.out.println(tile.data.get(i).size());
		}
		*/
		long end = System.currentTimeMillis();
		//System.out.println("time to build: "+(end - start) +"ms");
	}
	
	public List<Double> executeQuery(String arrayname, Params p) throws IOException {
		List<Double> myresult = new ArrayList<Double>();
		String[] cmd = buildCmd(arrayname, p);
		
		// print command
		//UtilityFunctions.printStringArray(cmd);
		//System.out.println();
		
		Process proc = Runtime.getRuntime().exec(cmd);
		/*
		// only uncomment this if things aren't working
		BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		for (String line; (line = ebr.readLine()) != null;) {
			System.out.println(line);
		}
		*/
		long start = System.currentTimeMillis();
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        boolean first = true;
		for (String line; (line = br.readLine()) != null;) {
			//System.out.println(line);
			if(!first) { // ignore first line
				String[] tokens = line.split(",");
				for(int i = 0; i < tokens.length; i++) {
					myresult.add(Double.parseDouble(tokens[i]));
				}
			} else {
				first = false;
			}
		}
		long end = System.currentTimeMillis();
		//System.out.println("time to build: "+(end - start) +"ms");
		return myresult;
	}
	
	public Tile getTile(TileKey id) {
		List<Double> input = this.getTileFromDatabase(id);
		byte[] data = getBytes(input);
		return new Tile(id,data);
	}
	
	public List<Double> getTileFromDatabase(TileKey id) {
		List<Double> myresult = new ArrayList<Double>();
		String tile_id = id.buildTileString();
		if(tile_id == null) {
			System.out.println("could not build tile_id");
			return myresult;
		}
		Map<Integer,Params> map1 = this.paramsMap.get(id.buildTileString());
		if(map1 == null) {
			System.out.println("map1 is null");
			return myresult;
		}
		Params p = map1.get(id.zoom);
		if(p == null) {
			System.out.println("params is null");
			return myresult;
		}
		myresult = this.executeQuery(p);
		return myresult;
	}
	
	public byte[] getBytes(List<Double> data) {
		long start = System.currentTimeMillis();
		byte [] result = new byte[8*data.size()];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		for(int i = 0; i < data.size(); i++) {
			buffer.putDouble(i*8, data.get(i));
		}
		long end = System.currentTimeMillis();
		//System.out.println("Time to convert to bytes: "+(end-start)+"ms");
		return result;
	}
	
	public static void main(String[] args) {
		boolean nicetest = false;
		boolean tiletest = false;
		boolean writetest = true;
		boolean readtest = false;
		Params p = new Params();
		p.xmin = 0;
		p.ymin = 0;
		p.xmax = 3600;
		p.ymax = 1800;//1697;
		p.width = 9;
		ScidbTileInterface sti = new ScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		String idstr = "[0, 0]";
		int zoom = 0;
		List<Integer> tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		TileKey id = new TileKey(tile_id,zoom);
		
		if(tiletest) {
			Tile result = sti.getTile(id);
			
			System.out.println(result.getDataSize());
			double[] histogram = result.getHistogramSignature();
			if(histogram != null && (histogram.length > 0)) {
				System.out.println("successfully built histogram");
			}
			//double[] norm = result.getNormalSignature();
			//double[] fhistogram = result.getFilteredHistogramSignature();
		}
		
		if(nicetest) {
			NiceTile testtile = sti.getNiceTile(id);
			System.out.println("total points: "+testtile.data.get(0).size());
			System.out.println(testtile.attributes.get(0)+","+testtile.data.get(0).get(100));
			System.out.println(testtile.attributes.get(1)+","+testtile.data.get(1).get(100));
			System.out.println(testtile.attributes.get(2)+","+testtile.data.get(2).get(100));
		}
		
		if(writetest) {
			NiceTile result = sti.getNiceTile(id);
			System.out.println("result length: "+result.data.get(0).size()+", total attributes: "+result.attributes.size());
			boolean success = sti.writeNiceTile(result);
			System.out.println("successfully wrote tile "+id.buildTileStringForFile()+" to disk? "+success);
		}
		
		if(readtest) {
			NiceTile result = sti.readNiceTile(id);
			System.out.println("result length: "+result.data.get(0).size()+", total attributes: "+result.attributes.size());
		}
	}

}
