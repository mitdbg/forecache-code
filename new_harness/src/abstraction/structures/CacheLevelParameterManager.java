package abstraction.structures;

import abstraction.enums.CacheLevel;

public class CacheLevelParameterManager {
	public String [] tasknames = null;
	public int[] user_ids = null;
	
	public CacheLevel level = null;
    public String[][] models = null;
    public int[][] allocations = null;
    public boolean[] usePhases = null;
    public int[] neighborhoods = null;
    
    public double accuracy;
    public String[] fullAccuracy;
    
    
	/*
	 *      <arg line="10015 151,148 task1,task2,task3 sift-dsift/ngram3 8-8/10 [neighborhood/...] false/false"/>
     *      <arg line="10015 151,148 task1,task2,task3 ngram3,sift/sift 4,4/8 [neighborhood/...] true/false"/>
	 */
	
    public CacheLevelParameterManager(String userstring, String taskstring, CacheLevel level, String modelstring,
			String allocationstring, String neighborhoodstring, String phasestring) throws ParameterException {
    	this.level = level;
    	parseInputs(userstring, taskstring, modelstring, allocationstring, neighborhoodstring, phasestring);
    }
    
    public void parseNeighborhoods(String neighborhoodstring) throws ParameterException {
    	String[] tempflags = neighborhoodstring.split("-"); // for each model combo
		if(tempflags.length != models.length) {
			throw new ParameterException("incorrect number of neighborhoods passed for cache level "+level);
		}
		neighborhoods = new int[tempflags.length];
		for(int i = 0; i < tempflags.length; i++) {
			neighborhoods[i] = Integer.parseInt(tempflags[i]);
			//System.out.println("adding neighborhood: "+neighborhoods[i]);
		}
    }
    
	public void parseUsers(String userstring) {
		String[] useridstrs = userstring.split(",");
		user_ids = new int[useridstrs.length];
		for(int i = 0; i < useridstrs.length; i++) {
			user_ids[i] = Integer.parseInt(useridstrs[i]);
			//System.out.println("adding user: "+user_ids[i]);
		}
	}
	
	public void parseTasks(String taskstring) {
		String[] taskstrs = taskstring.split(",");
		tasknames = new String[taskstrs.length];
		for(int i = 0; i < taskstrs.length; i++) {
			tasknames[i] = taskstrs[i];
			//System.out.println("adding task: "+tasknames[i]);
		}
	}
	
	public void parseModels(String modelstring) {
		String[] modelstrs = modelstring.split("-");
        models = new String[modelstrs.length][];
        for(int i = 0; i < modelstrs.length; i++) {
                String[] temp = modelstrs[i].split(",");
                models[i] = new String[temp.length];
                //System.out.print("adding model combination:");
                for(int j=0; j < temp.length;j++) {
                        models[i][j] = temp[j];
                        //System.out.print(" "+models[i][j]);
                }
                //System.out.println();
        }
	}
	
	public void parseAllocations(String allocationstring) throws ParameterException {
		if(allocationstring.equals("")) {
			allocations = new int [0][];
			return;
		}
		String[] tempallocations = allocationstring.split("-"); // for each model combo
		if(tempallocations.length != models.length) {
			throw new ParameterException("incorrect number of allocations for cache level "+level);
		}
		allocations = new int[tempallocations.length][];
		for(int i = 0; i < tempallocations.length; i++) {
			 String[] temp = tempallocations[i].split(",");
                allocations[i] = new int[temp.length];
                //System.out.print("adding allocation combination:");
                for(int j=0; j < temp.length;j++) {
                        allocations[i][j] = Integer.parseInt(temp[j]);
                        //System.out.print(" "+allocations[i][j]);
                }
                //System.out.println();
		}
	}
	
	public void parsePhases(String phasestring) throws ParameterException {
		String[] tempflags = phasestring.split("-"); // for each model combo
		if(tempflags.length != models.length) {
			throw new ParameterException("incorrect number of usePhase flags for cache level "+level);
		}
		usePhases = new boolean[tempflags.length];
		for(int i = 0; i < tempflags.length; i++) {
			usePhases[i] = Boolean.parseBoolean(tempflags[i]);
			//System.out.println("adding phase usage: "+usePhases[i]);
		}
	}
	
	public void parseInputs(String userstring, String taskstring, String modelstring,
			String allocationstring, String neighborhoodstring, String phasestring) throws ParameterException {
		parseUsers(userstring);
		parseTasks(taskstring);
		parseModels(modelstring);
		parseAllocations(allocationstring);
		parsePhases(phasestring);
		parseNeighborhoods(neighborhoodstring);
	}
	
	public static class ParameterException extends Exception {
		/**
		 * auto-generated id
		 */
		private static final long serialVersionUID = -7763527640960704614L;

		public ParameterException(String message) {
			super(message);
		}
	}
}
