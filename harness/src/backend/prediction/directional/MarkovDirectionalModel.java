package backend.prediction.directional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.TileKey;

import utils.UserRequest;
import utils.UtilityFunctions;

public class MarkovDirectionalModel extends BasicModel {
	protected Map<String,MDMNode> condprobs;
	
	public MarkovDirectionalModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len) {
		super(ref,membuf,diskbuf,api,len);
		condprobs = new HashMap<String,MDMNode>();
	}
	
	@Override
	public List<DirectionPrediction> predictOrder(List<UserRequest> htrace) throws Exception {
		return super.predictOrder(htrace,true);
	}
	
	@Override
	public double computeConfidence(Direction d, List<UserRequest> trace) {
		if(trace.size() <= 1) { // if there is no prior direction
			return computeBaseProb(d);
		}
		// trace is at least 2 here
		int last = trace.size() - 1;
		int nextlast = last - 1;
		String prefix = buildDirectionString(trace);
		double prior = 1.0;
		MDMNode node = condprobs.get(prefix);
		if(node != null) {
			Double prob = node.probability.get(d);
			if(prob != null) { // exact match
				return prob;
			} else { // find longest relevant prefix and extrapolate
				// get the last direction taken
				Direction subd = UtilityFunctions.getDirection(trace.get(nextlast),trace.get(last));
				prior = computeConfidence(subd,trace.subList(1, trace.size()));
				return computeBaseProb(d) * prior;
			}
			
		} else { // find longest relevant prefix and extrapolate
			// get the last direction taken
			Direction subd = UtilityFunctions.getDirection(trace.get(nextlast),trace.get(last));
			prior = computeConfidence(subd,trace.subList(1, trace.size()));
			return computeBaseProb(d) * prior;
		}
	}
	
	public double computeBaseProb(Direction d) {
		MDMNode node = condprobs.get("");
		if(node != null) {
			Double prob = node.probability.get(d);
			if(prob != null) { // exact match
				return prob;
			} else {
				return defaultprob;
			}
		} else {
			return defaultprob;
		}
	}
	
	protected void updateCondProbs(String prefix, Direction d) {
		MDMNode node = condprobs.get(prefix);
		if(node == null) {
			node = new MDMNode();
			condprobs.put(prefix,node);
		}
		node.count++;
		Double prob = node.probability.get(d);
		if(prob == null) {
			node.probability.put(d, 1.0);
		} else {
			node.probability.put(d, prob+1);
		}
	}
	
	public void train(List<UserRequest> trace) {
		if(trace.size() < 2) {
			return;
		}
		String dirstring = "";
		int i = 1;
		UserRequest n = trace.get(0);
		//System.out.println("n:"+n.tile_id+","+n.zoom);
		while(i < trace.size()) {
			UserRequest p = n;
			n = trace.get(i);
			Direction d = UtilityFunctions.getDirection(p,n);
			if(d != null) {
				dirstring += d;
			} else {
				dirstring = ""; // reset, user went back to [0,0],0
			}
			if(dirstring.length() > this.len) { // head of string changed
				dirstring = dirstring.substring(1);
			}
			if(dirstring.length() > 0) {
				for(int j = dirstring.length()-1; j >= 0; j--) {
					String sub = dirstring.substring(j);
					//System.out.println("sub: " +sub);
					if(sub.length() > 1) {
						String prefix = sub.substring(0,sub.length()-1);
						//System.out.println(prefix+"-"+d);
						this.updateCondProbs(prefix,d);
						
					} else { // single char
						this.updateCondProbs("",d);
					}
				}
			}
			//System.out.println("dirstring: " +dirstring);

			i++;
		}/*
		for(String key : frequencies.keySet()) {
			System.out.println("key: "+key+", count: "+frequencies.get(key));
		}*/
	}
	
	// call this last, after all train calls have happened
	public void finishTraining() {
		learnProbabilities();
	}
	
	// call this last, after all train calls have happened
	public void learnProbabilities() {
		//make the actual probabilities
		for(String key : condprobs.keySet()) {
			//System.out.println("prefix: "+key);
			MDMNode node = condprobs.get(key);
			for(Direction dkey : node.probability.keySet()) {
				Double prob = node.probability.get(dkey);
				node.probability.put(dkey, prob/node.count);
				//System.out.println("conditional probability for "+dkey+": "+(prob/node.count));
			}
		}
	}
	
	public void predictTest() {
		if(this.history == null) {
			return;
		}
		long start = System.currentTimeMillis();
		List<UserRequest> htrace = history.getHistoryTrace(this.len);
		String dirstring = buildDirectionString(htrace);
		TileKey prediction = null;
		System.out.println("dirstring: "+dirstring);
		for(int i = 0; i < dirstring.length(); i++) {
			String sub = dirstring.substring(i);
			MDMNode node = condprobs.get(sub);
			if(node != null) {
				Direction d = null;
				double mprob = 0;
				for(Direction dkey : node.probability.keySet()) {
					double cprob = node.probability.get(dkey);
					if((d == null) || (cprob > mprob)) {
						d = dkey;
						mprob = cprob;
					}
				}
				System.out.println("found hit at prefix: '"+sub+"'");
				System.out.println("'"+d+"' is the most likely next direction: "+mprob);
				int end = this.len - 1;
				if(end >= htrace.size()) {
					end = htrace.size() - 1;
				}
				prediction = this.DirectionToTile(htrace.get(end),d);
				if(prediction != null) {
					break;
				}
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("time to predict: "+(end-start)+"ms");
	}
	
	protected class MDMNode {
		public int count = 0;
		public Map<Direction,Double> probability;
		
		MDMNode() {
			this.probability = new HashMap<Direction,Double>();
		}
	}
}
