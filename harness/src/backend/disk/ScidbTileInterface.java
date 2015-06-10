package backend.disk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.scidb.jdbc.IResultSetWrapper;
import org.scidb.jdbc.IStatementWrapper;


import backend.util.NiceTile;
import backend.util.Params;
import backend.util.TileKey;
import utils.DBInterface;
import utils.UtilityFunctions;

public class ScidbTileInterface extends TileInterface {
	public static int simulation_build_delay = 30000; // in ms
	
	public ScidbTileInterface() {
		super();
	}
	
	public ScidbTileInterface(String paramsfile, String delim) {
		super(paramsfile,delim);
	}
	
	@Override
	public synchronized NiceTile getNiceTile(TileKey id) {
		NiceTile tile = new NiceTile(id);
		getStoredTile(DBInterface.arrayname,tile);
		if(tile.getSize() == 0) {
			buildAndStoreTile(DBInterface.arrayname,id);
			getStoredTile(DBInterface.arrayname,tile);
		}
		return tile;
	}
	
	public synchronized String buildShowQuery(String arrayname, Params p, TileKey id) {
		String tile_name = super.getStoredTileName(arrayname, id);
		return "show("+buildQuery(arrayname,p)+","+tile_name+")";
	}
	
	public synchronized String buildGetStoredTileQuery(String arrayname) {
		return "scan("+arrayname+")";
	}
	
	public synchronized String buildStoreTileQuery(String arrayname, Params p, TileKey id) {
		String tile_name = super.getStoredTileName(arrayname, id);
		return "store("+buildQuery(arrayname,p)+","+tile_name+")";
	}
	
	// inserts parameters into aggregation query
	public synchronized String buildQuery(String arrayname,Params p) {
		String query = "regrid(subarray("+arrayname+","+p.xmin+","+p.ymin+","+p.xmax+","+p.ymax+"),"+p.width+","+p.width+
				",avg(ndsi) as avg_ndsi,min(ndsi) as min_ndsi,max(ndsi) as max_ndsi,max(land_sea_mask) as max_land_sea_mask)";
		//System.out.println("query: "+query);
		return query;
	}
	
	public synchronized String buildInitQuery() {
		return "project(list('arrays'),name)";
	}
	
	public synchronized String[] buildCmdNoOutput(String query) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER=13.3 ; " +
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; " +
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; " +
				"source ~/.bashrc ; iquery -c " + DBInterface.scidb_host + " -anq \"" + query + "\"";
		return myresult;
	}
	
	public synchronized String[] buildCmd(String query) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER=13.3 ; " +
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; " +
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; " +
				"source ~/.bashrc ; iquery -c " + DBInterface.scidb_host + " -o csv+ -aq \"" + query + "\"";
		return myresult;
	}
	
	public synchronized String[] buildMeasureCmd(String query) {
		String[] myresult = new String[3];
		myresult[0] = "bash";
		myresult[1] = "-c";
		myresult[2] = "export SCIDB_VER=13.3 ; " +
				"export PATH=/opt/scidb/$SCIDB_VER/bin:/opt/scidb/$SCIDB_VER/share/scidb:$PATH ; " +
				"export LD_LIBRARY_PATH=/opt/scidb/$SCIDB_VER/lib:$LD_LIBRARY_PATH ; " +
				"source ~/.bashrc ; iquery -c " + DBInterface.scidb_host + " -o csv -aq \"" + query + "\" | head -n 0";
		return myresult;
	}
	
	public synchronized List<String> getArrayNames() {
		List<String> arrayNames = new ArrayList<String>();
		String query = buildInitQuery();
		String[] cmd = buildCmd(query);
			try {
				//System.out.println("query: \""+query+"\"");
				Process proc = Runtime.getRuntime().exec(cmd);

				boolean first = true;
				BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				for (String line; (line = ebr.readLine()) != null;) {
					if(first) {
						first = false;
					} else {
						String[] tokens = line.split(",");
						arrayNames.add(tokens[1]);
					}
					//System.out.println(line);
				}
				ebr.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return arrayNames;
	}
	
	public synchronized long removeStoredTile(String arrayname, TileKey id) {
		long start = System.currentTimeMillis();
		String query = "remove("+super.getStoredTileName(arrayname, id)+")";
		String[] cmd = buildCmdNoOutput(query);
			try {
				System.out.println("query: \""+query+"\"");
				Process proc = Runtime.getRuntime().exec(cmd);
				
				// this forces java to wait for the process to finish
				BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				for (String line; (line = ebr.readLine()) != null;) {
					System.out.println(line);
				}
				ebr.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		long end = System.currentTimeMillis();
		return end - start;
	}
	
	public synchronized long MeasureTile(String arrayname, TileKey id) {
		long start = System.currentTimeMillis();
		Params p = paramsMap.getParams(id);
		String query = buildQuery(arrayname,p);
		String[] cmd = buildMeasureCmd(query);
			try {
				System.out.println("query: \""+query+"\"");
				Process proc = Runtime.getRuntime().exec(cmd);
				
				// this forces java to wait for the process to finish
				BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				for (String line; (line = ebr.readLine()) != null;) {
					//System.out.println(line);
				}
				ebr.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		long end = System.currentTimeMillis();
		return end - start;
	}
	
	public synchronized long buildAndStoreTile(String arrayname, TileKey id) {
		long start = System.currentTimeMillis();
		Params p = paramsMap.getParams(id);
		String query = buildStoreTileQuery(arrayname,p,id);
		String[] cmd = buildCmdNoOutput(query);
			try {
				System.out.println("query: \""+query+"\"");
				Process proc = Runtime.getRuntime().exec(cmd);
				
				// this forces java to wait for the process to finish
				BufferedReader ebr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				for (String line; (line = ebr.readLine()) != null;) {
					System.out.println(line);
				}
				ebr.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		long end = System.currentTimeMillis();
		return end - start;
	}
	
	// use this to simulate cooking the tile. Cooking should take
	// significantly longer than fetching
	// so this should work fine.
	public synchronized long getSimulatedBuildTile(String arrayname, NiceTile tile) throws InterruptedException {
		long s = System.currentTimeMillis(); // start of the method
		long delay = getStoredTile(arrayname, tile);
		// subtract time required to retrieve from DBMS
		long finalsleep = simulation_build_delay + s - System.currentTimeMillis();
		if(finalsleep > 0) Thread.sleep(finalsleep);
		return delay;
	}
	
	public synchronized long getStoredTile(String arrayname, NiceTile tile) {
		List<Double> temp = new ArrayList<Double>();
		String[] labels = new String[0];
		long start = System.currentTimeMillis();
		String query = buildGetStoredTileQuery(super.getStoredTileName(arrayname, tile.id));
		String[] cmd = buildCmd(query);
		Process proc = null;
		try {
			//System.out.println("query: \""+query+"\"");
			proc = Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(proc != null) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				boolean first = true;
				for (String line; (line = br.readLine()) != null;) {
					//System.out.println("line: "+line);
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
		}

		long end = System.currentTimeMillis();
		return end - start;
	}
	
	public synchronized void executeQuery(String arrayname, Params p, NiceTile tile) {
		List<Double> temp = new ArrayList<Double>();
		String[] labels = new String[0];
		//long start = System.currentTimeMillis();
		
		Connection conn = DBInterface.getDefaultScidbConnection();
		if(conn == null) {
			return;
		}
	      
		try {
			conn.setAutoCommit(false);
			Statement st = conn.createStatement();
		      IStatementWrapper stWrapper = st.unwrap(IStatementWrapper.class);
		      stWrapper.setAfl(true);
		      System.out.println(buildQuery(arrayname,p));
			ResultSet rs = st.executeQuery(buildQuery(arrayname,p));
		    //IResultSetWrapper resWrapper = rs.unwrap(IResultSetWrapper.class);
		    ResultSetMetaData rsmd = rs.getMetaData();
			labels = new String[rsmd.getColumnCount()];
			for(int i = 0; i < labels.length; i++) {
				labels[i] = rsmd.getColumnName(i+1);
			}
			while(rs.next()) {
				//System.out.println("here");
				for(int i = 0; i < labels.length; i++) {
					temp.add(rs.getDouble(i+1));
				}
			}
			
			rs.close();
			st.close();
		} catch (SQLException e) {
			System.out.println("error occured while executing SciDB query");
			e.printStackTrace();
		}
		tile.initializeData(temp, labels);
		//long end = System.currentTimeMillis();
	}
	
	public static void main(String[] args) {
		boolean tiletest = true;
		Params p = new Params();
		p.xmin = 0;
		p.ymin = 0;
		p.xmax = 3600;
		p.ymax = 1800;//1697;
		p.width = 9;
		ScidbTileInterface nsti = new ScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		OldScidbTileInterface sti = new OldScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		String idstr = "[0, 0]";
		int zoom = 0;
		int[] tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		TileKey id = new TileKey(tile_id,zoom);
		NiceTile testtile = new NiceTile();
		testtile.id = id;
		//vti.executeQuery("thesis2",p,testtile);
		testtile = nsti.getNiceTile(id);
		System.out.println(testtile.getDataSize());
		
		if(tiletest) {
			double[] result = nsti.getNiceTile(id).data;
			double[] stiresult = sti.getNiceTile(id).data;
			for(int i = 0; i < 10; i++) {
				// this output should match the ScidbTileInterface output
				System.out.print(result[i*10 + 5000]+" ");
			}
			System.out.println();
			for(int i = 0; i < 10; i++) {
				//ScidbTileInterface output
				System.out.print(stiresult[i*10 + 5000]+" ");
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
	}
}
