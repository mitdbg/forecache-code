package frontend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBInterface {
	/*
	public static String cache_root_dir = "/home/leilani/_scalar_cache_dir2";
	public static String sig_root_dir = "/home/leilani/_scalar_sig_dir2";
	public static String csv_root_dir = "/home/leilani/_scalar_csv_dir2";
	public static String dbname  = "scalar";
	public static String user  = "leilani_testuser";
	public static String dim1 = "dims.latitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
	public static String dim2  = "dims.longitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
	public static String query  = "select * from ndsi_agg_7_18_2013";
	public static String hashed_query  = "2a0cf5267692de290efac7e3b6d5a593";
	public static String threshold  = "90000";
	 */
	public static String cache_root_dir = "/home/leibatt/projects/user_study/scalar_backend/_scalar_cache_dir2";
	public static String sig_root_dir = "/home/leibatt/projects/user_study/scalar_backend/_scalar_sig_dir2";
	public static String csv_root_dir = "/home/leibatt/projects/user_study/scalar_backend/_scalar_csv_dir2";
	public static String dbname  = "test";
	public static String user  = "testuser";
	public static String dim1 = "dims.xthesis2";
	public static String dim2  = "dims.ythesis2";
	public static String query  = "select * from thesis2";
	public static String hashed_query  = "85794fe89a8b0c23ce726cca7655c8bc";
	public static String threshold  = "90000";
	
	public static String warmup_query  = "select * from cali100";
	public static String warmup_hashed_query  = "39df90e13a84cad54463717b24ef833a";


	public static String password = "password";
	public static String host = "127.0.0.1";
	public static String port = "5432";
	public static String attr1  = "attrs.avg_ndsi";
	public static String attr2  = "attrs.max_land_sea_mask";

	public static double [] distance_threshold = {0,1,1,2,2,3,3,4,4};

	// filter by land sea mask value
	public static double [] fv = {1,7};


	public static String get_hashed_traces =
			"SELECT a.tile_id,b.tile_hash,a.zoom_level " +
					"FROM user_traces as a, tile_hash as b " +
					"WHERE a.tile_id = b.tile_id and " +
					"a.user_id=? and a.taskname=? and a.query='"+query+"'" +
					"ORDER BY id";

	// query for retrieving client-requested tiles for a specific user and task
	public static String get_user_traces = 
			"SELECT tile_id,zoom_level " +
					"FROM user_traces " +
					"WHERE user_id=? and taskname=? and query='"+query+"'" +
					"ORDER BY id";

	// query for seeing if user completed a specific task
	public static String check_task =
			"SELECT count(*) " +
					"FROM user_tile_selections " +
					"WHERE user_id=? and taskname=? and query='"+query+"'";

	// query for retrieving all users
	public static String get_users =
			"SELECT id FROM users";

	public static String get_tile_id = 
			"SELECT tile_id " +
					"FROM tile_hash " +
					"WHERE tile_hash=?";

	//get all user id's from database
	public static List<Integer> getUsers(Connection conn) {
		List<Integer> myresult = new ArrayList<Integer>();
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_users);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				myresult.add(rs.getInt(1));
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println("error occured while getting user id's");
			e.printStackTrace();
		}
		return myresult;
	}

	public static List<UserRequest> getUserTraces(Connection conn, int user_id, String taskname) {
		List<UserRequest> myresult = new ArrayList<UserRequest>();
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_user_traces);
			ps.setInt(1, user_id);
			ps.setString(2, taskname);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String tile_id = rs.getString(1);
				int zoom = rs.getInt(2);
				UserRequest ur = new UserRequest(tile_id, zoom);
				myresult.add(ur);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println("error occured while getting trace for user '" + user_id + "'");
			e.printStackTrace();
		}
		return myresult;
	}
	
	public static boolean checkTask(Connection conn, int user_id, String taskname) {
		boolean myresult = false;
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(check_task);
			ps.setInt(1, user_id);
			ps.setString(2, taskname);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				int count = rs.getInt(1);
				myresult = count > 0;
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println("error occured while checking task '" + taskname + "' for user '" + user_id + "'");
			e.printStackTrace();
		}
		return myresult;
	}
	
	
	public static List<UserRequest> getHashedTraces(Connection conn, int user_id, String taskname) {
		List<UserRequest> myresult = new ArrayList<UserRequest>();
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_hashed_traces);
			ps.setInt(1, user_id);
			ps.setString(2, taskname);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String tile_id = rs.getString(1);
				String tile_hash = rs.getString(2);
				int zoom = rs.getInt(3);
				UserRequest ur = new UserRequest(tile_id, zoom, tile_hash);
				myresult.add(ur);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println("error occured while getting hash trace for user " + user_id + "'");
			e.printStackTrace();
		}
		return myresult;
	}
	
	public static String getTileId(Connection conn, String tile_hash) {
		String myresult = "";
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_tile_id);
			ps.setString(1,tile_hash);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				myresult = rs.getString(1);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println("error occured while getting tile id for hash: '" + tile_hash + "'");
			e.printStackTrace();
		}
		return myresult;
	}
	
	public static List<Double> getPosition(Connection conn, String tile_hash) {
		List<Double> myresult = new ArrayList<Double>();
		String tile_id = getTileId(conn,tile_hash);
		String stripped = tile_id.substring(1, tile_id.length() - 1);
		String [] tokens = stripped.split(",");
		try {
			for(int i = 0; i < tokens.length; i++) {
				if(tokens[i].length() > 0) {
	
						myresult.add(Double.parseDouble(tokens[i]));
				}
			}
		} catch (NumberFormatException e) {
			System.out.println("error occured while converting string to double");
		}
		return myresult;
	}

	public static Connection getConnection() {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(
					"jdbc:postgresql://" + host + ":" + port + "/" + dbname,
					user,
					password
					);
		} catch (SQLException e) {
			System.out.println("error opening connection to database '" + dbname + "' as user '" + user + "'");
			e.printStackTrace();
		}
		return conn;
	}



	/*
	 * Local class for storing information about individual tile requests
	 * 
	 */
	public static class UserRequest {
		public String tile_id;
		public String tile_hash;
		public int zoom;

		public UserRequest(String tile_id,int zoom) {
			this.tile_id = tile_id;
			this.zoom = zoom;
			this.tile_hash = "";
		}
		
		public UserRequest(String tile_id, int zoom, String tile_hash) {
			this(tile_id,zoom);
			this.tile_hash = tile_hash;
			
		}
	}

}
