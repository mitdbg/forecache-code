package implementation.mimic.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserMap {
	private Map<String,Long> currentUsers;
	private long prunetime = 300000L; // prune if five minutes has gone by
	
	public UserMap() {
		currentUsers = new HashMap<String,Long>();
	}
	
	public synchronized Long get(String user) {
		return currentUsers.get(user);
	}
	
	public synchronized void put(String user) {
		currentUsers.put(user, System.currentTimeMillis());
	}
	
	public synchronized void remove(String user) {
		currentUsers.remove(user);
	}
	
	public synchronized void prune() {
		long curr = System.currentTimeMillis();
		List<String> toRemove = new ArrayList<String>();
		for(String user : currentUsers.keySet()) {
			Long lastUpdate = currentUsers.get(user);
			if((curr - lastUpdate) > prunetime) {
				toRemove.add(user);
			}
		}
		
		for(String user : toRemove) {
			currentUsers.remove(user);
		}
	}
}