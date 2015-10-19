package abstraction.storage;

import abstraction.structures.NewTileKey;

public interface EvictionPolicy {
	// called in insert function
	public boolean evaluateInsert(NewTileKey key);
	
	// called in get function
	public boolean evaluateGet(NewTileKey key);
	
	// called in remove() function
	public NewTileKey getNextToRemove();
	
	// called in remove(key) function
	public boolean evaluateRemove(NewTileKey key);
}
