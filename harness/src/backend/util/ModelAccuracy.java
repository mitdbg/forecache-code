package backend.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.UtilityFunctions;

public class ModelAccuracy {
	private Map<Model,String[]> modelAccuracyMap;
	private String[] labels;
	private int tracelength = -1;
	
	public ModelAccuracy() {
		this.modelAccuracyMap = new HashMap<Model,String[]>();
	}
	
	public void addModel(Model m, String accuracy) {
		addModel(m, parseAccuracyString(accuracy));
	}
	
	public void addModel(Model m, String[] hm) {
		this.modelAccuracyMap.put(m, hm);
		if(this.tracelength < 0) {
			this.tracelength = this.modelAccuracyMap.get(m).length;
		}
	}
	
	private String[] parseAccuracyString(String accuracy) {
		String[] hitmiss = accuracy.split(",");
		return hitmiss;
	}
	
	// using the given window length (maxlen), figure out
	// how successful the current model is at each time step
	public int[] buildTrackRecord(Model m, int maxlen) {
		String[] hm = this.modelAccuracyMap.get(m);
		if(hm == null) {
			return null;
		}
		List<String> hist = new ArrayList<String>();
		int[] ret = new int[hm.length];
		for(int i = 0; i < hm.length; i++) {
			for(int j = 0; j < hist.size(); j++) {
				if(hist.get(j).equals("hit")) {
					ret[i]++;
				}
			}
			hist.add(hm[i]);
			if(hist.size() > maxlen) {
				hist.remove(0);
			}
		}
		return ret;
	}
	
	public void buildTrackRecords(int maxlen) {
		List<Model> models = new ArrayList<Model>(this.modelAccuracyMap.keySet());
		Collections.sort(models);
		for(Model m : models) {
			int[] results = buildTrackRecord(m,maxlen);
			System.out.print(m+" success rate:");
			UtilityFunctions.printIntArray(results);
		}
	}
	
	public void learnSimpleModelLabels() {
		this.labels = new String[this.tracelength];
		List<Model> models = new ArrayList<Model>(this.modelAccuracyMap.keySet());
		Collections.sort(models);
		
		for(int i = 0; i < this.tracelength; i++) {
			StringBuilder label = new StringBuilder();
			label.append("[");
			boolean first = true;
			for(Model m : models) {
				if(this.modelAccuracyMap.get(m)[i].equals("hit")) {
					if(!first) {
						label.append("-");
					} else {
						first = false;
					}
					label.append(m);
				}
			}
			label.append("]");
			this.labels[i] = label.toString();
			if(this.labels[i].equals("[]")) {
				this.labels[i] = "[none]";
			}
		}
		System.out.print("simple labels:");
		UtilityFunctions.printStringArray(this.labels);
		System.out.println();
	}
	
	public void learnModelLabels() {
		boolean bflag, sflag, nflag;
		this.labels = new String[this.tracelength];
		for(int i = 0; i < this.tracelength; i++) {
			String label = "none";
			nflag = this.modelAccuracyMap.containsKey(Model.MARKOV)
					&& this.modelAccuracyMap.get(Model.MARKOV)[i].equals("hit");

			int b1 = this.modelAccuracyMap.containsKey(Model.MOMENTUM) 
					&& this.modelAccuracyMap.get(Model.MOMENTUM)[i].equals("hit") ? 1 : 0;
			int b2 = this.modelAccuracyMap.containsKey(Model.HOTSPOT) 
					&& this.modelAccuracyMap.get(Model.HOTSPOT)[i].equals("hit") ? 1 : 0;
			bflag = (b1 + b2) > 0;
			
			int s0 = this.modelAccuracyMap.containsKey(Model.NORMAL) 
					&& this.modelAccuracyMap.get(Model.NORMAL)[i].equals("hit") ? 1 : 0;
			int s1 = this.modelAccuracyMap.containsKey(Model.HISTOGRAM) 
					&& this.modelAccuracyMap.get(Model.HISTOGRAM)[i].equals("hit") ? 1 : 0;
			int s2 = this.modelAccuracyMap.containsKey(Model.FHISTOGRAM) 
					&& this.modelAccuracyMap.get(Model.FHISTOGRAM)[i].equals("hit") ? 1 : 0;
			sflag = (s0 + s1 + s2) >= 2;
			
			if (bflag && sflag) {
				label = "all";
			} else if(nflag) {
				label = "ngram only";
			} else if (bflag) {
				label = "behavioral models only";
			} else if (sflag) {
				label = "statistical models only";
			}
			this.labels[i] = label;
		}
		System.out.print("labels:");
		UtilityFunctions.printStringArray(this.labels);
		System.out.println();
	}
}
