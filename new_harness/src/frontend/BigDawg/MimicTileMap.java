package frontend.BigDawg;

import java.util.HashMap;
import java.util.Map;

public class MimicTileMap {
	private Map<String,String> tileMap;
	
	public MimicTileMap() {
		this.tileMap = new HashMap<String,String>();
	}
	
	public synchronized void push(String jobid, String data) {
		tileMap.put(jobid, data);
	}
	
	public synchronized String pop(String jobid) {
		String returnval = tileMap.get(jobid);
		if(returnval != null) {
			tileMap.remove(jobid);
		}
		return returnval;
	}
	
	public synchronized boolean peek(String jobid) {
		return tileMap.containsKey(jobid);
	}
}
