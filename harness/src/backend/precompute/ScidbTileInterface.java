package backend.precompute;

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
import backend.util.Signatures;
import backend.util.Tile;
import backend.util.TileKey;

/*
 * calls SciDB from command line
 */
public class ScidbTileInterface {
	private Map<String,Map<Integer,Params>> paramsMap;
	public static String defaultparamsfile = "/home/leibatt/projects/user_study/scalar_backend/thesis2_params.tsv";
	public static String defualtdelim = "\t";
	private String paramsfile;
	private String delim;
	
	public ScidbTileInterface(String paramsfile, String delim) {
		this.paramsfile = paramsfile;
		this.delim = delim;
		this.paramsMap = new HashMap<String, Map<Integer, Params>>();
		parseParamsFile();
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
	
	public void parseParamsFile() {
		File pf = new File(this.paramsfile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(pf));
			for(String line; (line = br.readLine()) != null;) {
				String[] tokens = line.split(delim);
				if(tokens.length != 7) {
					System.out.println("Error in params file. line does not have correct number of items!");
					return;
				}
				String tile_id = "";
				int zoom = -1;
				Params p = new Params();
				for(int i = 0; i < tokens.length; i++) {
					switch(i) {
						case 0: tile_id = tokens[i];
								//System.out.println("tile id: '"+tile_id+"'");
								break;
						case 1: zoom = Integer.parseInt(tokens[i]);
								//System.out.println("zoom: "+zoom);
								break;
						case 2: p.xmin = Integer.parseInt(tokens[i]);
								break;
						case 3: p.ymin = Integer.parseInt(tokens[i]);
								break;
						case 4: p.xmax = Integer.parseInt(tokens[i]);
								break;
						case 5: p.ymax = Integer.parseInt(tokens[i]);
								break;
						case 6: p.width = Integer.parseInt(tokens[i]);
								break;
						default:
					}
				}
				if((zoom >= 0) && (tile_id != null)) {
					//System.out.println("tile id: '"+tile_id+"'");
					//System.out.println("zoom: "+zoom);
					Map<Integer, Params> temp = this.paramsMap.get(tile_id);
					if(temp == null) {
						temp = new HashMap<Integer, Params>();
						this.paramsMap.put(tile_id,temp);
					}
					temp.put(zoom, p);
				}
			}
		} catch (IOException e) {
			System.out.println("error occured while reading params file");
			e.printStackTrace();
		}

	}
	
	public static void main(String[] args) {
		Params p = new Params();
		p.xmin = 0;
		p.ymin = 0;
		p.xmax = 3600;
		p.ymax = 1800;//1697;
		p.width = 9;
		ScidbTileInterface sti = new ScidbTileInterface(ScidbTileInterface.defaultparamsfile,ScidbTileInterface.defualtdelim);
		String idstr = "[0, 0]";
		int zoom = 0;
		List<Integer> tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		TileKey id = new TileKey(tile_id,zoom);
		Tile result = sti.getTile(id);
		//double[] histogram = result.getHistogramSignature();
		//double[] norm = result.getNormalSignature();
		//double[] fhistogram = result.getFilteredHistogramSignature();
		System.out.println("enum down: " + Direction.DOWN);
	}

}
