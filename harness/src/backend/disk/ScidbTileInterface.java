package backend.disk;

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
	
	/*
	 * A
	 * 1 - xmin
	 * 2 - width
	 * 3 - ymin
	 * 4 - width
	 * 
	 * "5" - arrayname
	 * 
	 * B
	 * 6 - xmax
	 * 7 - xmin
	 * 8 - ymax
	 * 9 - ymin
	 */
	// inserts parameters into aggregation query
	public synchronized String buildQuery(String arrayname,Params p) {
		String query = "regrid(subarray("+arrayname+","+p.xmin+","+p.ymin+","+p.xmax+","+p.ymax+"),"+p.width+","+p.width+
				",avg(ndsi) as avg_ndsi,min(ndsi) as min_ndsi,max(ndsi) as max_ndsi,max(land_sea_mask) as max_land_sea_mask)";
		//System.out.println("query: "+query);
		return query;
	}
	
	public ScidbTileInterface() {
		super();
	}
	
	public ScidbTileInterface(String paramsfile, String delim) {
		super(paramsfile,delim);
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
			Statement st = conn.createStatement();
		      IStatementWrapper stWrapper = st.unwrap(IStatementWrapper.class);
		      stWrapper.setAfl(true);
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
			System.out.println("error occured while executing vertica query");
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
