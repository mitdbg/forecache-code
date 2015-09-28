package abstraction.util;

import java.util.List;

import abstraction.util.DirectionClass;

public class TraceMetadata {
	public List<ExplorationPhase> explorationPhases; // exploration phase for each request
	public List<DirectionClass> directionClasses; // direction class for each request
	public List<Boolean> matches; // TODO: make this better
}
