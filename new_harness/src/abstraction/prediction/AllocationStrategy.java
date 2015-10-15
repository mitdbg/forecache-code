package abstraction.prediction;

import abstraction.util.Model;

public class AllocationStrategy {
	public Model[] models;
	public int[] allocations;
	
	public AllocationStrategy(Model[] models,int[] allocations) {
		this.models = models;
		this.allocations = allocations;
	}
	
	public int getAllocation(int i) {
		return allocations[i];
	}
	
	public Model getModel(int i) {
		return models[i];
	}
}
