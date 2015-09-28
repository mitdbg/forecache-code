package abstraction.prediction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import abstraction.util.Model;

public class AllocationStrategyMap {
	protected Map<ExplorationPhase,Allocation> allocationMap;
	protected List<Model> models;
	
	public AllocationStrategyMap(List<Model> models) {
		this.allocationMap = new HashMap<ExplorationPhase,Allocation>();
		this.models = models;
	}
	
	public void insertAllocation(ExplorationPhase phase,Allocation allocation) {
		this.allocationMap.put(phase, allocation);
	}
	
	public Allocation getAllocation(ExplorationPhase phase) {
		return this.allocationMap.get(phase);
	}
	
	public void clear() {
		this.allocationMap.clear();
	}
}
