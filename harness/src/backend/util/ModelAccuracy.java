package backend.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelAccuracy {
	private Map<Model,String[]> modelAccuracyMap;
	private String[] labels;
	private int tracelength = -1;
	
	public ModelAccuracy() {
		this.modelAccuracyMap = new HashMap<Model,String[]>();
	}
	
	public void addModel(Model m, String accuracy) {
		this.modelAccuracyMap.put(m, parseAccuracyString(accuracy));
		if(this.tracelength < 0) {
			this.tracelength = this.modelAccuracyMap.get(m).length;
		}
	}
	
	private String[] parseAccuracyString(String accuracy) {
		String[] hitmiss = accuracy.split(",");
		return hitmiss;
	}
	
	public void learnSimpleModelLabels() {
		this.labels = new String[this.tracelength];
		List<Model> models = new ArrayList<Model>(this.modelAccuracyMap.keySet());
		Collections.sort(models);
		
		for(int i = 0; i < this.tracelength; i++) {
			StringBuilder label = new StringBuilder();
			boolean first = true;
			for(Model m : models) {
				if(this.modelAccuracyMap.get(m)[i].equals("hit")) {
					if(!first) {
						label.append(",");
					} else {
						first = false;
					}
					label.append(m);
				}
			}
			
			this.labels[i] = label.toString();
		}
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
			
			if(nflag) {
				label = "ngram only";
			} else if (bflag && sflag) {
				label = "all";
			} else if (bflag) {
				label = "behavioral models only";
			} else if (sflag) {
				label = "statistical models only";
			}
			this.labels[i] = label;
		}
	}
}
