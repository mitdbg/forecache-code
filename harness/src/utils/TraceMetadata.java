package utils;

import java.util.List;

import backend.util.DirectionClass;

public class TraceMetadata {
	public List<ExplorationPhase> explorationPhases; // exploration phase for each request
	public List<DirectionClass> directionClasses; // direction class for each request
	public List<Boolean> matches; // TODO: make this better
}
