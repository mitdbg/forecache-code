package abstraction.structures;

import abstraction.enums.Model;
import abstraction.prediction.AllocationStrategyMap;

/**
 * @author leibatt
 * used to store the necessary metadata for a given caching layer
 * so we can use one prediction engine to compute predictions
 * over different layers
 */
public class SessionMetadata {
	// history
	public String userid = null; // which user is this?
	public int historylength = -1; // n
	public TileHistoryQueue history = null; // last n tile requests, last ROI, etc.
	public AllocationStrategyMap allocationStrategyMap = null;
	
	public SessionMetadata(String userid, int maxhist,
			AllocationStrategyMap allocationStrategyMap) {
		this.userid = userid;
		this.historylength = maxhist;
		this.history = new TileHistoryQueue(maxhist);
		this.allocationStrategyMap = allocationStrategyMap;
	}
	
	public static int[] parseUserStrings(String[] userstrs) {
		int[] argusers = new int[userstrs.length];
		for(int i = 0; i < userstrs.length; i++) {
			System.out.println("userstrs["+i+"] = '"+userstrs[i]+"'");
			argusers[i] = Integer.parseInt(userstrs[i]);
		}
		return argusers;
	}
	
	public static ModelSetup parseModelStrings(String[] modelstrs, int baselen) {
		Model[] modellabels = new Model[modelstrs.length];
		int[] historylengths = new int[modelstrs.length];
		for(int i = 0; i < modelstrs.length; i++) {
			System.out.println("modelstrs["+i+"] = '"+modelstrs[i]+"'");
			historylengths[i] = baselen;
			if(modelstrs[i].contains("ngram")) {
				modellabels[i] = Model.NGRAM;
				historylengths[i] = Integer.parseInt(modelstrs[i].substring(5));
			} else if(modelstrs[i].equals("random")) {
				modellabels[i] = Model.RANDOM;
			} else if(modelstrs[i].contains("hotspot")) {
				modellabels[i] = Model.HOTSPOT;
				if(modelstrs[i].length() > 7) {
					historylengths[i] = Integer.parseInt(modelstrs[i].substring(7));
				}
			} else if(modelstrs[i].contains("momentum")) {
				modellabels[i] = Model.MOMENTUM;
				if(modelstrs[i].length() > 8) {
					historylengths[i] = Integer.parseInt(modelstrs[i].substring(8));
				}
			} else if(modelstrs[i].equals("normal")) {
				modellabels[i] = Model.NORMAL;
			} else if(modelstrs[i].equals("histogram")) {
				modellabels[i] = Model.HISTOGRAM;
			} else if(modelstrs[i].equals("fhistogram")) {
				modellabels[i] = Model.FHISTOGRAM;
			} else if(modelstrs[i].equals("sift")) {
				modellabels[i] = Model.SIFT;
			} else if (modelstrs[i].equals("dsift")) {
				modellabels[i] = Model.DSIFT;
			}
		}
		ModelSetup returnval = new ModelSetup();
		returnval.modellabels = modellabels;
		returnval.historylengths = historylengths;
		return returnval;
	}
	
	public static int[] parseAllocationStrings(String[] allocationstrings) {
		int[] allocations = new int[allocationstrings.length];
		for(int i = 0; i < allocations.length; i++) {
			allocations[i] = Integer.parseInt(allocationstrings[i]);
		}
		return allocations;
	}
	
	/*********************** nested classes *************************/
	public static class ModelSetup {
		public Model[] modellabels;
		public int[] historylengths;
	}
}
