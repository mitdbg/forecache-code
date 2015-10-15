package abstraction.prediction;

import java.util.HashMap;
import java.util.Map;

import abstraction.util.Model;

public class AllocationStrategyMap {
	protected Map<AnalysisPhase,AllocationStrategy> allocationMap;
	
	public AllocationStrategyMap(Model[] models) {
		this.allocationMap = new HashMap<AnalysisPhase,AllocationStrategy>();
	}
	
	public void put(AnalysisPhase phase,AllocationStrategy allocation) {
		this.allocationMap.put(phase, allocation);
	}
	
	public AllocationStrategy get(AnalysisPhase phase) {
		return this.allocationMap.get(phase);
	}
	
	public void clear() {
		this.allocationMap.clear();
	}
}
