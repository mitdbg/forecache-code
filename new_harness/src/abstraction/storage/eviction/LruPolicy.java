package abstraction.storage.eviction;

import java.util.ArrayList;
import java.util.List;

import abstraction.storage.EvictionPolicy;
import abstraction.structures.NewTileKey;

public class LruPolicy implements EvictionPolicy {
	List<NewTileKey> queue;
	
	public LruPolicy() {
		this.queue = new ArrayList<NewTileKey>();
	}

	@Override
	public boolean evaluateInsert(NewTileKey key) {
		evaluateRemove(key); // remove previous instance of key
		queue.add(key); // add key to the end of the queue
		return true;
	}

	@Override
	public boolean evaluateGet(NewTileKey key) {
		return evaluateInsert(key);
	}

	@Override
	public NewTileKey getNextToRemove() {
		if(queue.size() > 0) {
			return queue.get(0);
		} else {
			return null;
		}
	}

	@Override
	public boolean evaluateRemove(NewTileKey key) {
		int pos = queue.indexOf(key);
		if(pos >= 0) { // remove key
			queue.remove(pos);
		}
		return false;
	}

}
