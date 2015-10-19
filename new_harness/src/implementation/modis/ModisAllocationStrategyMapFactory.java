package implementation.modis;

import implementation.AllocationStrategyMapFactory;
import abstraction.enums.Model;
import abstraction.prediction.AllocationStrategy;
import abstraction.prediction.AllocationStrategyMap;
import abstraction.prediction.AnalysisPhase;

public class ModisAllocationStrategyMapFactory extends AllocationStrategyMapFactory {
	
	// allocation strategy described in the 2015 technical report
	public static AllocationStrategyMap get2ModelHybridMap(int totalAllocations) {
		Model[] models = {Model.NGRAM,Model.SIFT};
		AllocationStrategyMap asm = new AllocationStrategyMap(models);
		// use this allocation strategy for every phase
		AllocationStrategy sensemakingStrategy = new AllocationStrategy(models, new int[]{0,totalAllocations});
		int[] allocations = new int[2];
		if(totalAllocations < 4) {
			allocations[0] = totalAllocations;
			allocations[1] = 0;
		} else {
			allocations[0] = 4;
			allocations[1] = totalAllocations-4;
		}
		AllocationStrategy foragingStrategy = new AllocationStrategy(models, allocations);
		AllocationStrategy navigationStrategy = new AllocationStrategy(models, allocations);
		asm.put(AnalysisPhase.SENSEMAKING, sensemakingStrategy);
		asm.put(AnalysisPhase.FORAGING, foragingStrategy);
		asm.put(AnalysisPhase.NAVIGATION, navigationStrategy);
		return asm;
	}
}
