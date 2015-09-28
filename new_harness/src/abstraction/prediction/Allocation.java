package abstraction.prediction;

import java.util.List;

import abstraction.util.Model;

public class Allocation {
	public List<Model> models;
	public List<Integer> allocations;
	public List<Integer> indices; // the ordering for these models in the tile cache
	
	public Allocation(List<Model> models,List<Integer> allocations, List<Integer> indices) {
		this.models = models;
		this.allocations = allocations;
		this.indices = indices;
	}
}
