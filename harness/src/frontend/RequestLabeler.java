package frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import utils.DBInterface;
import utils.ExplorationPhase;
import utils.TraceMetadata;
import utils.UserRequest;
import utils.UtilityFunctions;
import backend.util.Direction;
import backend.util.DirectionClass;
import backend.util.History;

public class RequestLabeler {
	List<UserRequest> points = null;
	List<DirectionClass> dirs = null;

	
	public RequestLabeler() {
		points = new ArrayList<UserRequest>();
		dirs = new ArrayList<DirectionClass>();
	}
	
	// handles every possible Exploration phase case, given the previous, current, and next direction class values
	public static ExplorationPhase getExplorationPhase(DirectionClass prev, DirectionClass next) {
		if(next == null) {
			switch(prev) {
			case IN: return ExplorationPhase.ANALYZE;
			case PAN: return ExplorationPhase.ANALYZE;
			case OUT: return ExplorationPhase.ID;
			default:
				return null;
			}
		}
		switch(prev) {
		case IN:
			if(next == DirectionClass.IN) {
				return ExplorationPhase.TRANS;
			}
				return ExplorationPhase.ANALYZE;
		case PAN:
			if(next == DirectionClass.IN) {
				return ExplorationPhase.ID;
			}
			return ExplorationPhase.ANALYZE;
		case OUT:
			if(next == DirectionClass.IN) {
				return ExplorationPhase.ID;
			}
			return ExplorationPhase.TRANS;
		default:
			return null;
		}
	}
	
	public static List<DirectionClass> getDirectionClasses(List<UserRequest> trace) {
		List<DirectionClass> dirs = new ArrayList<DirectionClass>();
		if(trace.size() <= 1) return dirs;
		UserRequest prev = trace.get(0);
		for(int i = 1; i < trace.size(); i++) {
			UserRequest next = trace.get(i);
			int zoomdiff = prev.zoom - next.zoom;
			if(zoomdiff < 0) { // zoom level increased
				dirs.add(DirectionClass.IN);
			} else if (zoomdiff > 0) { // zoom level decreased
				dirs.add(DirectionClass.OUT);
			} else {
				dirs.add(DirectionClass.PAN);
			}
			prev = next;
		}
		return dirs;
	}
	
	public static List<ExplorationPhase> getExplorationPhases(List<DirectionClass> dirs) {
		List<ExplorationPhase> phases = new ArrayList<ExplorationPhase>();
		phases.add(ExplorationPhase.ID);
		if(dirs.size() == 0) return phases;
		DirectionClass prev = dirs.get(0);
		for(int i = 1; i < dirs.size(); i++) {
			DirectionClass next = dirs.get(i);
			phases.add(getExplorationPhase(prev,next));
			prev = next;
		}
		phases.add(getExplorationPhase(prev,null));
		return phases;
	}
	
	public static List<ExplorationPhase> getExplorationPhaseLabelsFromTrace(List<UserRequest> trace) {
		List<DirectionClass> dirs = getDirectionClasses(trace);
		return getExplorationPhases(dirs);
	}
	
	public static void getMatchLabels(TraceMetadata metadata) {
		do1(metadata);
	}
	
	public static void do1(TraceMetadata metadata) {
		List<DirectionClass> dirs = metadata.directionClasses;
		metadata.matches = new ArrayList<Boolean>();
		int index = 2;
		int bad = 0;
		if(dirs.size() == 0) {
			System.out.println("no direction info");
			return;
		}
		System.out.println("dirs size: "+dirs.size());
		for(int i = 0; i < dirs.size(); i++) {
			DirectionClass curr = dirs.get(i);
			if(index == 2) {
				//System.out.println("bleh");
				switch(curr) {
				case IN: index = 0; break;
				default:
					index = 2;
				}
				metadata.matches.add(true);
			} else if (index == 1) {
				//System.out.println("in pan state");
				switch(curr) {
				case IN: index = 0;
					for(int j = 0; j < bad; j++) {metadata.matches.add(false);};
					metadata.matches.add(true);
					break;
				case PAN: index = 1; bad += 1;
					break;
				default: for(int j = 0; j <= bad; j++) {metadata.matches.add(true);};
					index = 2;
				}
			} else { // index == 0
				//System.out.println("in zoom in state");
				switch(curr) {
				case IN: metadata.matches.add(true); index = 0; break;
				case PAN: index = 1; bad = 1; break;
				default: metadata.matches.add(true); index = 2;
				}
			}
		}
		if(index == 1) {
			for(int i = 0; i < bad; i++) {
				metadata.matches.add(true);
			}
		}
	}
	
	public static void do2(TraceMetadata metadata) {
		List<DirectionClass> dirs = metadata.directionClasses;
		metadata.matches = new ArrayList<Boolean>();
		int index = 2;
		int bad = 0;
		if(dirs.size() == 0) return;
		for(int i = 1; i < dirs.size(); i++) {
			DirectionClass curr = dirs.get(0);
			if(index == 2) {
				switch(curr) {
				case IN: index = 0; break;
				default:
					index = 2;
				}
				metadata.matches.add(true);
			} else if (index == 1) {
				switch(curr) {
				case IN: index = 0; for(int j = 0; j < bad; j++) {metadata.matches.add(false);}; break;
				case OUT: metadata.matches.add(true); index = 2; break;
				case PAN: index = 1; bad += 1; break;
				default:
					return;
				}
			} else { // index == 0
				switch(curr) {
				case IN: metadata.matches.add(true); index = 0; break;
				case OUT: metadata.matches.add(true); index = 2; break;
				case PAN: index = 1; bad = 1; break;
				default:
					return;
				}
			}
		}
	}
	
	public static TraceMetadata getLabels(List<UserRequest> trace) {
		TraceMetadata metadata = new TraceMetadata();
		metadata.directionClasses = getDirectionClasses(trace);
		metadata.explorationPhases = getExplorationPhases(metadata.directionClasses);
		metadata.directionClasses.add(0, DirectionClass.START);
		//getMatchLabels(metadata);
		return metadata;
	}
	
	public static void printLabels(List<UserRequest> trace) {
		TraceMetadata metadata = getLabels(trace);
		List<DirectionClass> dirs = metadata.directionClasses;
		List<ExplorationPhase> phases = metadata.explorationPhases;
		//List<Boolean> matches = metadata.matches;
		if(trace.size() > 0) {
			System.out.println("request\tdirection-class\tphase\tmatch?");
			for(int i = 0; i < phases.size(); i++) {
				//System.out.println(trace.get(i)+"\t"+dirs.get(i)+"\t"+phases.get(i)+"\t"+matches.get(i));
				System.out.println(trace.get(i)+"\t"+dirs.get(i)+"\t"+phases.get(i));
			}
		}
	}
}
