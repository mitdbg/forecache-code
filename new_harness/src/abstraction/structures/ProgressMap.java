package abstraction.structures;

import java.util.HashMap;
import java.util.Map;


/*
 * Used to track what items/zoom levels are currently in flight
 */
public class ProgressMap<K,V> {
	private Map<K,V> itemMap;
	
	public ProgressMap() {
		this.itemMap = new HashMap<K,V>();
	}
	
	// add an in-progress item
	public synchronized void push(K key,V value) {
		itemMap.put(key, value);
	}
	
	// remove an in-progress item
	public synchronized V pop(K key) {
		V returnval = itemMap.get(key);
		if(returnval != null) {
			itemMap.remove(key);
		}
		return returnval;
	}
	
	// check whether the item is in progress
	public synchronized boolean peek(K key) {
		return itemMap.containsKey(key);
	}
}
