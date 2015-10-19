package implementation;

import abstraction.enums.Model;
import abstraction.prediction.AllocationStrategy;
import abstraction.prediction.AllocationStrategyMap;
import abstraction.prediction.AnalysisPhase;

public class AllocationStrategyMapFactory {
	
	// the allocations ignore the analysis phase
	public static AllocationStrategyMap getBasicMap(Model[] models, int[] allocations) {
		AllocationStrategyMap asm = new AllocationStrategyMap(models);
		// use this allocation strategy for every phase
		for(AnalysisPhase phase : AnalysisPhase.values()) {
			asm.put(phase, new AllocationStrategy(models, allocations));
		}
		return asm;
	}
}
