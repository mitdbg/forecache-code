package backend.disk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import backend.util.NiceTile;
import backend.util.Params;
import backend.util.TileKey;
import utils.DBInterface;
import utils.UtilityFunctions;

public class VerticaTileInterface extends TileInterface {
	
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
	public static final String vertica_tile_templateA = 
			"select floor((x-?)/?) as xbin, floor((y-?)/?) as ybin, " +
				"avg(ndsi) as avg_ndsi, min(ndsi) as min_ndsi, " +
				"max(ndsi) as max_ndsi, max(land_sea_mask) as max_land_sea_mask " +
				//"count(*) as count " +
				"from "/* table name goes here */;
	public static final String vertica_tile_templateB =
				" where x <= ? and x >= ? and y <= ? and y >= ? " +
				"group by xbin, ybin " +
				"order by xbin, ybin;";
	
	public VerticaTileInterface() {
		super();
	}
	
	public VerticaTileInterface(String paramsfile, String delim) {
		super(paramsfile,delim);
	}
	
	public static String insertTableName(String tablename) {
		return vertica_tile_templateA + tablename + vertica_tile_templateB;
	}
	
	public synchronized void executeQuery(String tablename, Params p, NiceTile tile) {
		List<Double> temp = new ArrayList<Double>();
		String[] labels = new String[0];
		//long start = System.currentTimeMillis();
		
		Connection conn = DBInterface.getDefaultVerticaConnection();
		if(conn == null) {
			return;
		}
		PreparedStatement ps;
		try {
			String template = insertTableName(tablename);
			ps = conn.prepareStatement(template);
			ps.setInt(1, p.xmin);
			ps.setInt(2, p.width);
			ps.setInt(3, p.ymin);
			ps.setInt(4, p.width);
			ps.setInt(5, p.xmax);
			ps.setInt(6, p.xmin);
			ps.setInt(7, p.ymax);
			ps.setInt(8, p.ymin);
			ResultSet rs = ps.executeQuery();
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
			ps.close();
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
		VerticaTileInterface vti = new VerticaTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		OldScidbTileInterface sti = new OldScidbTileInterface(DBInterface.defaultparamsfile,DBInterface.defaultdelim);
		String idstr = "[0, 0]";
		int zoom = 0;
		int[] tile_id = UtilityFunctions.parseTileIdInteger(idstr);
		TileKey id = new TileKey(tile_id,zoom);
		NiceTile testtile = new NiceTile();
		testtile.id = id;
		//vti.executeQuery("thesis2",p,testtile);
		testtile = vti.getNiceTile(id);
		System.out.println(testtile.getDataSize());
		
		if(tiletest) {
			double[] result = vti.getNiceTile(id).data;
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
