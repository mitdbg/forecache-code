package backend.disk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import backend.util.Params;
import backend.util.TileKey;
import utils.DBInterface;

public class PostgresTileInterface {
	/*
	 * 1 - xmin
	 * 2 - width
	 * 3 - ymin
	 * 4 - width
	 * 5 - xmax
	 * 6 - xmin
	 * 7 - ymax
	 * 8 - ymin
	 */
	private String postgres_tile_template = 
			"select (x-?)/? as xbin, (y-?)/? as ybin, " +
				"avg(ndsi) as avg_ndsi, min(ndsi) as min_ndsi, " +
				"max(ndsi) as max_ndsi, max(land_sea_mask) as max_land_sea_mask, " +
				"count(*) as count " +
				"from thesis2 " +
				"where x <= ? and x >= ? and y <= ? and y >= ? " +
				"group by xbin, ybin " +
				"order by xbin, ybin;";
	
	private String paramsfile = "/home/leibatt/projects/user_study/scalar_backend/thesis2_params.tsv";
	private String delim = "\t";
	private Map<String,Map<Integer,Params>> paramsMap;
	
	public PostgresTileInterface(String paramsfile, String delim) {
		this.paramsfile = paramsfile;
		this.delim = delim;
		this.paramsMap = new HashMap<String, Map<Integer, Params>>();
		parseParamsFile();
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
	
	public byte[] getTile(TileKey id) {
		List<Double> data = getTileFromDatabase(id);
		return getBytes(data);
	}
	
	public List<Double> getTileFromDatabase(TileKey id) {
		List<Double> myresult = new ArrayList<Double>();
		Connection conn = DBInterface.getConnection();
		if(conn == null) {
			return myresult;
		}
		String tile_id = id.buildTileString();
		if(tile_id == null) {
			return myresult;
		}
		Map<Integer,Params> map1 = this.paramsMap.get(id.buildTileString());
		if(map1 == null) {
			return myresult;
		}
		Params p = map1.get(id.zoom);
		if(p == null) {
			return myresult;
		}
		try {
			PreparedStatement ps = conn.prepareStatement(this.postgres_tile_template);
			ps.setInt(1, p.xmin);
			ps.setInt(2, p.width);
			ps.setInt(3, p.ymin);
			ps.setInt(4, p.width);
			ps.setInt(5, p.xmax);
			ps.setInt(6, p.xmin);
			ps.setInt(7, p.ymax);
			ps.setInt(8, p.ymin);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				for(int i = 0; i < 5; i++) {
					myresult.add(rs.getDouble(i));
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println("error occured while getting tile '" + tile_id + "' zoom "+id.zoom+" from database");
			e.printStackTrace();
		}
		return myresult;
	}
	
	public byte[] getBytes(List<Double> data) {
		byte [] result = new byte[8*data.size()];
		ByteBuffer buffer = ByteBuffer.wrap(result);
		for(int i = 0; i < data.size(); i++) {
			buffer.putDouble(i*8, data.get(i));
		}
		return result;
	}
}
