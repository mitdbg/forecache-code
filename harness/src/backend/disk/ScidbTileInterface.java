package backend.disk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.DBInterface;
import utils.UtilityFunctions;

import backend.util.Direction;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.Signatures;
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
		this.paramsMap = new ParamsMap(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}
	
	public ScidbTileInterface(String paramsfile, String delim) {
		this.paramsfile = paramsfile;
		this.delim = delim;
		this.paramsMap = new ParamsMap(paramsfile,delim);
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
		myresult[2] = "source ~/.bashrc; iquery -o csv+ -aq \"" + buildQuery(arrayname,p) + "\"";
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
	
	public List<Double> executeQuery(String arrayname, Params p) throws IOException {
		List<Double> myresult = new ArrayList<Double>();
		String[] cmd = buildCmd(arrayname, p);
		Process proc = Runtime.getRuntime().exec(cmd);
		//proc.getErrorStream();
		long start = System.currentTimeMillis();
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        boolean first = true;
		for (String line; (line = br.readLine()) != null;) {
				if(!first) { // ignore first line
					//System.out.println(line);
					String[] tokens = line.split(",");
					for(int i = 0; i < tokens.length; i++) {
						myresult.add(Double.parseDouble(tokens[i]));
					}
				} else {
					first = false;
				}
		}
		long end = System.currentTimeMillis();
		System.out.println("time to build: "+(end - start) +"ms");
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
			return myresult;
		}
		Map<Integer,Params> map1 = this.paramsMap.get(id.buildTileString());
		if(map1 == null) {
			System.out.println("map1 is null");
			return myresult;
		}
		Params p = map1.get(id.getZoom());
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
		System.out.println("Time to convert to bytes: "+(end-start)+"ms");
		return result;
	}
	
	public static void main(String[] args) {
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
		Tile result = sti.getTile(id);
		double[] histogram = result.getHistogramSignature();
		//double[] norm = result.getNormalSignature();
		//double[] fhistogram = result.getFilteredHistogramSignature();
		//System.out.println("enum down: " + Direction.DOWN);
	}

}
