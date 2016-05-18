package abstraction.mdstorage;

import abstraction.mdstructures.MultiDimTileKey;

public interface EvictionPolicy {
	// called in insert function
	public boolean evaluateInsert(MultiDimTileKey key);
	
	// called in get function
	public boolean evaluateGet(MultiDimTileKey key);
	
	// called in remove() function
	public MultiDimTileKey getNextToRemove();
	
	// called in remove(key) function
	public boolean evaluateRemove(MultiDimTileKey key);
}
