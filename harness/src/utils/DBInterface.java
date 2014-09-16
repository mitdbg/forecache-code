package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class DBInterface {
	
	public static String defaultparamsfile = "/home/leilani/csv/ndsi_agg_7_18_2013_params.tsv";
	public static final String cache_root_dir = "/home/leilani/test_cache";
	public static final String sig_root_dir = "/home/leilani/_scalar_sig_dir2";
	public static final String csv_root_dir = "/home/leilani/_scalar_csv_dir2";
	public static final String dbname  = "scalar";
	public static final String user  = "leilani_testuser";
	public static final String arrayname = "ndsi_agg_7_18_2013";
	public static final String dim1 = "dims.latitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
	public static final String dim2  = "dims.longitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
	public static final String query  = "select * from ndsi_agg_7_18_2013";
	public static final String hashed_query  = "2a0cf5267692de290efac7e3b6d5a593";
	public static final String threshold  = "90000";
	public static final int minuser = 121;
	
/*
	public static String defaultparamsfile = "/home/leibatt/projects/user_study/scalar_backend/thesis2_params.tsv";
	public static final String cache_root_dir = "/home/leibatt/projects/user_study/scalar_backend/test_cache";
	public static final String sig_root_dir = "/home/leibatt/projects/user_study/scalar_backend/_scalar_sig_dir2";
	public static final String csv_root_dir = "/home/leibatt/projects/user_study/scalar_backend/_scalar_csv_dir2";
	public static final String dbname  = "test";
	public static final String user  = "testuser";
	public static final String arrayname = "thesis2";
	public static final String dim1 = "dims.xthesis2";
	public static final String dim2  = "dims.ythesis2";
	public static final String query  = "select * from thesis2";
	public static final String hashed_query  = "85794fe89a8b0c23ce726cca7655c8bc";
	public static final String threshold  = "90000";
	public static final int minuser = 28;
*/	
	public static String defaultdelim = "\t";
	public static final String warmup_query  = "select * from cali100";
	public static final String warmup_hashed_query  = "39df90e13a84cad54463717b24ef833a";


	public static final String password = "password";
	public static final String host = "127.0.0.1";
	public static final String port = "5432";
	public static final String attr1  = "attrs.avg_ndsi";
	public static final String attr2  = "attrs.max_land_sea_mask";

	public static final double [] distance_threshold = {0,1,1,2,2,3,3,4,4};

	// filter by land sea mask value
	public static final double [] fv = {1,7};
	
	public static final String get_tile_hash =
			"SELECT tile_hash " +
			"FROM tile_hash " +
			"WHERE tile_id=?";

	public static final String get_hashed_traces =
			"SELECT a.tile_id,b.tile_hash,a.zoom_level " +
					"FROM user_traces as a, tile_hash as b " +
					"WHERE a.tile_id = b.tile_id and " +
					"a.user_id=? and a.taskname=? and a.threshold="+threshold+" and a.query='"+query+"' " +
					"ORDER BY id";

	// query for retrieving client-requested tiles for a specific user and task
	public static final String get_user_traces = 
			"SELECT tile_id,zoom_level " +
					"FROM user_traces " +
					//"WHERE user_id=? and taskname=\'?\' and threshold="+threshold+" and query='"+query+"' " +
					"WHERE user_id=? and taskname=? and threshold="+threshold+" and query='"+query+"' " +
					//"ORDER BY id";
					"ORDER BY timestamp";

	// query for seeing if user completed a specific task
	public static final String check_task =
			"SELECT count(*) " +
					"FROM user_tile_selections " +
					"WHERE user_id=? and taskname=? and threshold="+threshold+" and query='"+query+"'";

	// query for retrieving all users
	public static final String get_users =
			"SELECT id FROM users where id>="+minuser;
	
	public static final String get_users_from_traces =
			"SELECT distinct user_id FROM user_traces order by user_id";
	
	public static final String get_tasks_from_traces =
			"SELECT distinct taskname FROM user_traces order by taskname";

	public static final String get_tile_id = 
			"SELECT tile_id " +
					"FROM tile_hash " +
					"WHERE tile_hash=?";

	//get all user id's from database
	public static List<Integer> getUsers() {
		List<Integer> myresult = new ArrayList<Integer>();
		Connection conn = getConnection();
		if(conn == null) {
			return myresult;
		}
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
		
		finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return myresult;
	}
	
	public static List<Integer> getUsersFromTraces() {
		List<Integer> myresult = new ArrayList<Integer>();
		Connection conn = getConnection();
		if(conn == null) {
			return myresult;
		}
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_users_from_traces);
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
		
		finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return myresult;
	}
	
	public static List<String> getTasksFromTraces() {
		List<String> myresult = new ArrayList<String>();
		Connection conn = getConnection();
		if(conn == null) {
			return myresult;
		}
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_tasks_from_traces);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				myresult.add(rs.getString(1));
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println("error occured while getting user id's");
			e.printStackTrace();
		}
		
		finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return myresult;
	}

	public static List<UserRequest> getUserTraces(int user_id, String taskname) {
		List<UserRequest> myresult = new ArrayList<UserRequest>();
		Connection conn = getConnection();
		if(conn == null) {
			return myresult;
		}
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
		finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return myresult;
	}
	
	public static boolean checkTask(int user_id, String taskname) {
		boolean myresult = false;
		Connection conn = getConnection();
		if(conn == null) {
			return myresult;
		}
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
		finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return myresult;
	}
	
	
	public static List<UserRequest> getHashedTraces(int user_id, String taskname) {
		List<UserRequest> myresult = new ArrayList<UserRequest>();
		Connection conn = getConnection();
		if(conn == null) {
			return myresult;
		}
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
		finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return myresult;
	}
	
	public static String getTileHash(String tile_id) {
		String myresult = "";
		Connection conn = getConnection();
		if(conn == null) {
			return myresult;
		}
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_tile_hash);
			ps.setString(1,tile_id);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				myresult = rs.getString(1);
			}
			rs.close();
			ps.close();
		} catch (SQLException e) {
			System.out.println("error occured while getting tile hash for tile id: '" + tile_id + "'");
			e.printStackTrace();
		}
		finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return myresult;
	}
	
	public static String getTileId(String tile_hash) {
		String myresult = "";
		Connection conn = getConnection();
		if(conn == null) {
			return myresult;
		}
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
		finally {
			if(conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return myresult;
	}
	
	public static List<Double> getPosition(String tile_hash) {
		String tile_id = getTileId(tile_hash);
		List<Double> myresult = UtilityFunctions.parseTileIdDouble(tile_id);
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

}
