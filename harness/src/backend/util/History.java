package backend.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.UtilityFunctions;

public class History {
	private List<Direction> hist;
	private int maxhist = 10;
	
	public History() {
		this.hist = new ArrayList<Direction>();
	}
	
	public History(int maxhist) {
		this();
		this.maxhist = maxhist;
	}
	
	public void add(Direction dir) {
		this.hist.add(dir);
		if(this.hist.size() > this.maxhist) { // shift history
			this.hist.remove(0);
		}
	}
	
	public Map<DirectionClass,Integer> getClassDistribution() {
		Map<DirectionClass,Integer> resultMap = new HashMap<DirectionClass,Integer>();
		for(Direction dir : this.hist) {
			DirectionClass dc = UtilityFunctions.getDirectionClass(dir);
			Integer count = resultMap.get(dc);
			if(count == null) {
				resultMap.put(dc, 1);
			} else {
				resultMap.put(dc,count+1);
			}
		}
		return resultMap;
	}
	
	public static String getClassDistributionString(Map<DirectionClass,Integer> resultMap) {
		StringBuilder result = new StringBuilder();
		 for(DirectionClass dc : resultMap.keySet()) {
			 Integer count = resultMap.get(dc);
			 result.append("Count(");
			 result.append(dc);
			 result.append(") = ");
			 result.append(count);
			 result.append("\n");
		 }
		 return result.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder returnval = new StringBuilder(Math.max(this.hist.size() * 2 - 1,0));
		if(this.hist.size() > 0) {
			returnval.append(this.hist.get(0));
		}
		for(int i = 1; i < this.hist.size(); i++) {
			returnval.append(",");
			returnval.append(this.hist.get(i));
		}
		return returnval.toString();
	}
}
