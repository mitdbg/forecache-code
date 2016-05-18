package abstraction.mdstorage.eviction;

import java.util.ArrayList;
import java.util.List;

import abstraction.mdstorage.EvictionPolicy;
import abstraction.mdstructures.MultiDimTileKey;

public class LruPolicy implements EvictionPolicy {
	List<MultiDimTileKey> queue;
	
	public LruPolicy() {
		this.queue = new ArrayList<MultiDimTileKey>();
	}

	@Override
	public boolean evaluateInsert(MultiDimTileKey key) {
		evaluateRemove(key); // remove previous instance of key
		queue.add(key); // add key to the end of the queue
		return true;
	}

	@Override
	public boolean evaluateGet(MultiDimTileKey key) {
		return evaluateInsert(key);
	}

	@Override
	public MultiDimTileKey getNextToRemove() {
		if(queue.size() > 0) {
			return queue.get(0);
		} else {
			return null;
		}
	}

	@Override
	public boolean evaluateRemove(MultiDimTileKey key) {
		int pos = queue.indexOf(key);
		if(pos >= 0) { // remove key
			queue.remove(pos);
		}
		return false;
	}

}
