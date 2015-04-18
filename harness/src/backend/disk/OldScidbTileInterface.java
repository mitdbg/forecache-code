package backend.disk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.scidb.jdbc.Statement;

import utils.DBInterface;
import utils.UtilityFunctions;

import backend.util.NiceTile;
import backend.util.Params;
import backend.util.ParamsMap;
import backend.util.TileKey;

/*
 * calls SciDB from command line
 */
public class OldScidbTileInterface extends TileInterface {
	
	public OldScidbTileInterface() {
		this(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
	}
	
	public OldScidbTileInterface(String paramsfile, String delim) {
		this.paramsfile = paramsfile;
		this.delim = delim;
		this.paramsMap = new ParamsMap(this.paramsfile,this.delim);
	}
	
	// inserts parameters into aggregation query
	public synchronized String buildQuery(String arrayname,Params p) {
		String query = "regrid(subarray("+arrayname+","+p.xmin+","+p.ymin+","+p.xmax+","+p.ymax+"),"+p.width+","+p.width+
				",avg(ndsi) as avg_ndsi,min(ndsi) as min_ndsi,max(ndsi) as max_ndsi,max(land_sea_mask) as max_land_sea_mask)";
		//System.out.println("query: "+query);
		return query;
	}
	
	public synchronized String[] buildCmd(String arrayname, Params p) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER=12.10 ; " +
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; " +
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; " +
				"source ~/.bashrc ; iquery -o csv+ -aq \"" + buildQuery(arrayname,p) + "\"";
		return myresult;
	}
	
	public synchronized void executeQuery(String arrayname, Params p, NiceTile tile) {
		String[] cmd = buildCmd(arrayname, p);
		
		// print command
		//UtilityFunctions.printStringArray(cmd);
		//System.out.println();
		
		Process proc;
		try {
			proc = Runtime.getRuntime().exec(cmd);
		/*
		// only uncomment this if things aren't working
		BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		for (String line; (line = ebr.readLine()) != null;) {
			System.out.println(line);
		}
		*/
		List<Double> temp = new ArrayList<Double>();
		String[] labels = new String[0];
		//long start = System.currentTimeMillis();
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        boolean first = true;
			for (String line; (line = br.readLine()) != null;) {
				//System.out.println(line);
				String[] tokens = line.split(",");
				if(!first) { // ignore first line
					for(int i = 0; i < tokens.length; i++) {
						Double next = 0.0;
						try {
							next = Double.parseDouble(tokens[i]);
						} catch (NumberFormatException e) {
							System.out.println("could not parse SciDB result, skipping...");
							e.printStackTrace();
						}
						temp.add(next); // just add 0.0 if we can't parse it
					}
				} else {
					first = false;
					labels = tokens;
				}
			}
			tile.initializeData(temp, labels);
		} catch (IOException e) {
			System.out.println("Error occurred while reading query output.");
			e.printStackTrace();
		}
		/*
		for(int i = 0; i < tile.attributes.size(); i++) {
			System.out.print(tile.attributes.get(i)+"\t");
			System.out.println(tile.data.get(i).size());
		}
		*/
		//long end = System.currentTimeMillis();
		//System.out.println("time to build: "+(end - start) +"ms");
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
		OldScidbTileInterface sti = new OldScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		String idstr = "[0, 0]";
		int zoom = 0;
		int[] tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		TileKey id = new TileKey(tile_id,zoom);
		
		if(tiletest) {
			NiceTile t = new NiceTile();
			sti.executeQuery(DBInterface.arrayname,p,t);
			double[] testresult = t.data;
			double[] result = sti.getNiceTile(id).data;
			for(int i = 0; i < 10; i++) {
				System.out.print(testresult[i*10+5000]+" ");
			}
			System.out.println();
			for(int i = 0; i < 10; i++) {
				System.out.print(result[i*10+5000]+" ");
			}
			System.out.println();
			
			//System.out.println(result.getDataSize());
			//double[] histogram = result.getHistogramSignature();
			//if(histogram != null && (histogram.length > 0)) {
			//	System.out.println("successfully built histogram");
			//}
			//double[] norm = result.getNormalSignature();
			//double[] fhistogram = result.getFilteredHistogramSignature();
		}
		
		if(nicetest) {
			NiceTile testtile = sti.getNiceTile(id);
			System.out.println("total points: "+testtile.getDataSize());
			System.out.println(testtile.attributes[0]+","+testtile.get(0,100));
			System.out.println(testtile.attributes[1]+","+testtile.get(1,100));
			System.out.println(testtile.attributes[2]+","+testtile.get(2,100));
		}
		
		if(writetest) {
			NiceTile result = sti.getNiceTile(id);
			System.out.print("labels:");
			UtilityFunctions.printObjectArray(result.attributes);
			System.out.println();
			System.out.println("result length: "+result.getDataSize()+", total attributes: "+result.attributes.length);
			boolean success = NiceTilePacker.writeNiceTile(result);
			System.out.println("successfully wrote tile "+id.buildTileStringForFile()+" to disk? "+success);
		}
		
		if(readtest) {
			NiceTile result = NiceTilePacker.readNiceTile(id);
			System.out.println("result length: "+result.getDataSize()+", total attributes: "+result.attributes.length);
		}
	}

}
