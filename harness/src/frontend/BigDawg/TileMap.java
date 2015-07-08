package frontend.BigDawg;

import java.util.HashMap;
import java.util.Map;

public class TileMap {
	private Map<String,byte[]> tileMap;
	
	public TileMap() {
		this.tileMap = new HashMap<String,byte[]>();
	}
	
	public synchronized void push(String jobid, byte[] data) {
		tileMap.put(jobid, data);
	}
	
	public synchronized byte[] pop(String jobid) {
		byte[]	returnval = tileMap.get(jobid);
		if(returnval != null) {
			tileMap.remove(jobid);
		}
		return returnval;
	}
	
	public synchronized boolean peek(String jobid) {
		return tileMap.containsKey(jobid);
	}
}
