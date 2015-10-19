package abstraction.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import abstraction.structures.UserRequest;


public class DBInterface {
	/*
	 //for big dawg
	public static String nice_tile_cache_dir = "/home/gridsan/lbattle/forecache-code/data/nice_tile_cache";
	//public static String nice_tile_cache_dir = "/home/gridsan/lbattle/forecache-code/data/vertica_nice_tile_cache";
	public static String defaultparamsfile = "/home/gridsan/lbattle/forecache-code/data/ndsi_agg_7_18_2013_params.tsv";
	public static String cache_root_dir = "/home/gridsan/lbattle/forecache-code/data/test_cache";
	public static String sig_root_dir = "/home/gridsan/lbattle/forecache-code/data/_scalar_sig_dir2";
	public static String csv_root_dir = "/home/gridsan/lbattle/forecache-code/data/_scalar_csv_dir2";
	public static String dbname  = "scalar";
	public static String user  = "leilani_testuser";
	public static String arrayname = "ndsi_agg_7_18_2013";
	public static String dim1 = "dims.latitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
	public static String dim2  = "dims.longitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
	public static String query  = "select * from ndsi_agg_7_18_2013";
	public static String hashed_query  = "2a0cf5267692de290efac7e3b6d5a593";
	public static String threshold  = "90000";
	public static int minuser = 121;
	public static String xdim = "longitude_e4ndsi_06_03_2013";
	public static String ydim = "latitude_e4ndsi_06_03_2013";
	public static String zattr = "avg_ndsi";
	public static String groundTruth = "/home/gridsan/lbattle/forecache-code/data/gt_updated.csv";
*/	
	
	/*
	public static String nice_tile_cache_dir = "/home/leilani/nice_tile_cache";
	//public static String nice_tile_cache_dir = "/home/leilani/vertica_nice_tile_cache";
	public static String defaultparamsfile = "/data/scidb/000/1/leilani/csv/ndsi_agg_7_18_2013_params.tsv";
	public static String cache_root_dir = "/home/leilani/test_cache";
	public static String sig_root_dir = "/home/leilani/_scalar_sig_dir2";
	public static String csv_root_dir = "/home/leilani/_scalar_csv_dir2";
	public static String dbname  = "scalar";
	public static String user  = "leilani_testuser";
	public static String arrayname = "ndsi_agg_7_18_2013";
	public static String dim1 = "dims.latitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
	public static String dim2  = "dims.longitude_e4ndsi_06_03_2013ndsi_agg_7_18_2013";
	public static String query  = "select * from ndsi_agg_7_18_2013";
	public static String hashed_query  = "2a0cf5267692de290efac7e3b6d5a593";
	public static String threshold  = "90000";
	public static int minuser = 121;
	public static String xdim = "longitude_e4ndsi_06_03_2013";
	public static String ydim = "latitude_e4ndsi_06_03_2013";
	public static String zattr = "avg_ndsi";
	public static String groundTruth = "/home/leilani/scalar-prefetch/gt_updated.csv";
*/	
	
	public static String mimic_views_folder;

	public static String nice_tile_cache_dir;// = "/home/leibatt/projects/user_study/scalar_backend/nice_tile_cache";
	//public static String nice_tile_cache_dir;// = "/home/leibatt/projects/user_study/scalar_backend/vertica_nice_tile_cache";
	public static String defaultparamsfile;// = "/home/leibatt/projects/user_study/scalar_backend/thesis2_params.tsv";
	public static String cache_root_dir;// = "/home/leibatt/projects/user_study/scalar_backend/test_cache";
	//public static String cache_root_dir;// = "/home/leibatt/projects/user_study/scalar_backend/test_cache2";
	public static String sig_root_dir;// = "/home/leibatt/projects/user_study/scalar_backend/_scalar_sig_dir2";
	public static String csv_root_dir;// = "/home/leibatt/projects/user_study/scalar_backend/_scalar_csv_dir2";
	public static String dbname;//  = "test";
	public static String user;//  = "testuser";
	public static String arrayname;// = "thesis2";
	public static String dim1;// = "dims.xthesis2";
	public static String dim2;//  = "dims.ythesis2";
	public static String query;//  = "select * from thesis2";
	public static String hashed_query;//  = "85794fe89a8b0c23ce726cca7655c8bc";
	public static String threshold;//  = "90000";
	public static int minuser;// = 28;
	public static String xdim;// = "x";
	public static String ydim;// = "y";
	public static String zattr;// = "avg_ndsi";
	public static String groundTruth;// = "/home/leibatt/projects/scalar-prefetch/gt_updated.csv";

	public static String defaultdelim;// = "\t";
	public static String warmup_query;//  = "select * from cali100";
	public static String warmup_hashed_query;//  = "39df90e13a84cad54463717b24ef833a";


	public static String password;// = "password";
	public static String host;// = "127.0.0.1";
	public static String port;// = "5432";
	public static String attr1;//  = "attrs.avg_ndsi";
	public static String attr2 ;// = "attrs.max_land_sea_mask";
	
	
	public static String vertica_host;// = "vertica-vm";
	public static String vertica_port;// = "5433";
	public static String vertica_dbname;// = "test";
	public static String vertica_user;// = "dbadmin";
	public static String vertica_password;// = "password";
	
	public static String scidb_host;// = "localhost";
	public static String scidb_port;// = "1239";
	public static String scidb_user;// = "scidb";
	public static String scidb_password;// = "scidb";

	public static double [] distance_threshold = {0,1,1,2,2,3,3,4,4};

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
					"a.user_id=? and a.taskname=? and a.threshold=? and a.query=? " +
					"ORDER BY id";

	// query for retrieving client-requested tiles for a specific user and task
	public static final String get_user_traces = 
			"SELECT tile_id,zoom_level " +
					"FROM user_traces " +
					"WHERE user_id=? and taskname=? and threshold=? and query=? " +
					//"ORDER BY id";
					"ORDER BY timestamp";

	// query for seeing if user completed a specific task
	public static final String check_task =
			"SELECT count(*) " +
					"FROM user_tile_selections " +
					"WHERE user_id=? and taskname=? and threshold=? and query=?";

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
		Connection conn = getDefaultPostgresqlConnection();
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
		Connection conn = getDefaultPostgresqlConnection();
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
		Connection conn = getDefaultPostgresqlConnection();
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
		Connection conn = getDefaultPostgresqlConnection();
		if(conn == null) {
			return myresult;
		}
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_user_traces);
			ps.setInt(1, user_id);
			ps.setString(2, taskname);
			ps.setInt(3, Integer.parseInt(threshold));
			ps.setString(4, query);
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
		Connection conn = getDefaultPostgresqlConnection();
		if(conn == null) {
			return myresult;
		}
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(check_task);
			ps.setInt(1, user_id);
			ps.setString(2, taskname);
			ps.setInt(3, Integer.parseInt(threshold));
			ps.setString(4, query);
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
		Connection conn = getDefaultPostgresqlConnection();
		if(conn == null) {
			return myresult;
		}
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(get_hashed_traces);
			ps.setInt(1, user_id);
			ps.setString(2, taskname);
			ps.setInt(3, Integer.parseInt(threshold));
			ps.setString(4, query);
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
		Connection conn = getDefaultPostgresqlConnection();
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
		Connection conn = getDefaultPostgresqlConnection();
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
	
	public static Connection getSciDBConnection(String host, String port, String user, String password) {
	    try
	    {
	      Class.forName("org.scidb.jdbc.Driver");
	    }
	    catch (ClassNotFoundException e)
	    {
	      System.out.println("Driver is not in the CLASSPATH -> " + e);
	    }
	    
		Connection conn = null;
		try {
			//Class.forName("com.vertica.Driver");
			conn = DriverManager.getConnection(
					"jdbc:scidb://" + host + ":" + port + "/",
					user,
					password
					);
		} catch (SQLException e) {
			System.out.println("error opening connection to database '" + dbname + "' as user '" + user + "'");
			e.printStackTrace();
		}/* catch (ClassNotFoundException e) {
			// Could not find the driver class. Likely an issue
			// with finding the .jar file.
			System.err.println("Could not find the JDBC driver class.");
			e.printStackTrace();
			return null;
		}*/
		return conn;
	}
	
	public static Connection getVerticaConnection(String host, String port, String dbname, String user, String password) {
		Connection conn = null;
		try {
			//Class.forName("com.vertica.Driver");
			conn = DriverManager.getConnection(
					"jdbc:vertica://" + host + ":" + port + "/" + dbname,
					user,
					password
					);
		} catch (SQLException e) {
			System.out.println("error opening connection to database '" + dbname + "' as user '" + user + "'");
			e.printStackTrace();
		}/* catch (ClassNotFoundException e) {
			// Could not find the driver class. Likely an issue
			// with finding the .jar file.
			System.err.println("Could not find the JDBC driver class.");
			e.printStackTrace();
			return null;
		}*/
		return conn;
	}
	
	public static Connection getDefaultScidbConnection() {
		return getSciDBConnection(scidb_host,scidb_port,scidb_user,scidb_password);
	}
	
	public static Connection getDefaultVerticaConnection() {
		return getVerticaConnection(vertica_host,vertica_port,vertica_dbname,vertica_user,vertica_password);
	}
	
	public static Connection getPostgresqlConnection(String host, String port, String dbname, String user, String password) {
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

	public static Connection getDefaultPostgresqlConnection() {
		return getPostgresqlConnection(host,port,dbname,user,password);
	}

}
