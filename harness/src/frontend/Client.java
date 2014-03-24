package frontend;

import java.sql.Connection;
import java.util.List;

import frontend.DBInterface.UserRequest;

public class Client {
	
	public static String [] tasknames = {"warmup", "task1", "task2", "task3"};
	
	public static void getTracesForUsers(Connection conn) {
		List<Integer> users = DBInterface.getUsers(conn);
		for(int u = 0; u < users.size(); u++) {
			int user_id = users.get(u);
			for(int t = 0; t < tasknames.length; t++) {
				String taskname = tasknames[t];
				if(DBInterface.checkTask(conn,user_id,taskname)) {
					System.out.println("user '" + user_id + "' completed task '" + taskname + "'");
					List<UserRequest> trace = DBInterface.getHashedTraces(conn,user_id,taskname);
					System.out.println("found trace of size " + trace.size() + " for task '" + taskname + "' and user '" + user_id + "'");
					for(int r = 0; r < trace.size(); r++) {
						String tile_id = trace.get(r).tile_id;
						System.out.println("tile id: '" +tile_id+ "'");
						
						// do stuff here to analyze trace
						// need to: retrieve tile from disk, compute signature
						// this should be done prior to analyzing user traces
						// eventually need to: find nearest neighbors of al ltiles
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Could not find JDBC driver");
			e.printStackTrace();
			return;
		}
		
		Connection conn = DBInterface.getConnection();
		if(conn == null) {
			return;
		}
		getTracesForUsers(conn);
	}
}
