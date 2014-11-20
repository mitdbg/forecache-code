package backend.prediction.directional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.io.ArpaLmReader;
import edu.berkeley.nlp.lm.map.NgramMap;
import edu.berkeley.nlp.lm.values.ProbBackoffPair;

import backend.disk.DiskNiceTileBuffer;
import backend.disk.DiskTileBuffer;
import backend.disk.ScidbTileInterface;
import backend.memory.MemoryNiceTileBuffer;
import backend.memory.MemoryTileBuffer;
import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import backend.prediction.TileHistoryQueue;
import backend.util.Direction;
import backend.util.NiceTile;
import backend.util.Signatures;
import backend.util.TileKey;

import utils.UserRequest;
import utils.UtilityFunctions;

public class NGramDirectionalModel extends BasicModel {
	protected static List<String> sentences;
	protected static StringWordIndexer wordIndexer;
	protected static ArrayEncodedProbBackoffLm<String> lm;

	public NGramDirectionalModel(TileHistoryQueue ref, MemoryNiceTileBuffer membuf, DiskNiceTileBuffer diskbuf,ScidbTileInterface api, int len) {
		super(ref,membuf,diskbuf,api,len);
		sentences = new ArrayList<String>();
		setupNgramModel();
	}
	
	protected void setupNgramModel() {
		sentences = new ArrayList<String>();
		wordIndexer = new StringWordIndexer();
		wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
		wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
		wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);
		lm = null;
	}
	
	// computes an ordering of all directions using confidence values
	@Override
	public List<DirectionPrediction> predictOrder(List<TileKey> htrace) {
		return super.predictOrder(htrace,true);
	}
	
	@Override
	public Double computeConfidence(TileKey id, List<TileKey> trace) {
		List<TileKey> traceCopy = new ArrayList<TileKey>();
		traceCopy.addAll(trace);
		TileKey prev = traceCopy.get(traceCopy.size() - 1);
		//List<TileKey> path = UtilityFunctions.buildPath2(prev, id); // build a path to this key
		List<TileKey> path = UtilityFunctions.buildPath(prev, id); // build a path to this key
		return computeConfidenceForPath(path,traceCopy);
	}
	
	@Override
	public Double computeDistance(TileKey id, List<TileKey> trace) {
		return null;
	}
	
	public Double computeConfidenceForPath(List<TileKey> path, List<TileKey> traceCopy) {
		List<Direction> dirPath = UtilityFunctions.buildDirectionPath(path);
		if(dirPath.size() == 1) {
			return computeConfidence(dirPath.get(0),traceCopy);
		}
		double prob = 0;
		for(int i = 0; i < dirPath.size(); i++) {
			Direction d = dirPath.get(i);
			prob += computeConfidence(d,traceCopy); // log probabilities
			traceCopy.remove(0);
			traceCopy.add(path.get(i+1));
		}
		return prob;
	}
	
	@Override
	public double computeConfidence(Direction d, List<TileKey> trace) {
		String p = buildDirectionStringFromKey(trace);
		p += " " + d.getWord();
		//System.out.println("dirstring: \""+p +"\"");
		String[] prefix = p.split(" ");
		int[] ngram = new int[prefix.length];
		for(int i = 0; i < prefix.length; i++) {
			ngram[i] = wordIndexer.getIndexPossiblyUnk(prefix[i]);
		}

		return lm.getLogProb(ngram,0,prefix.length);
	}
	
	//want sentences now, not words
	@Override
	public String buildDirectionStringFromKey(List<TileKey> trace) {
		if(trace.size() < 2) {
			return "";
		}
		String dirstring = "";
		int i = 1;
		TileKey n = trace.get(0);
		TileKey p = n;
		n = trace.get(i);
		Direction d = UtilityFunctions.getDirection(p,n);
		if(d != null) {
			dirstring += d.getWord();
		}
		i++;
		
		while(i < trace.size()) {
			p = n;
			n = trace.get(i);
			d = UtilityFunctions.getDirection(p,n);
			if(d != null) {
				dirstring += " " + d.getWord();
			}
			i++;
		}
		return dirstring;
	}
	
	//want sentences now, not words
	@Override
	public String buildDirectionStringFromString(List<UserRequest> trace) {
		if(trace.size() < 2) {
			return "";
		}
		String dirstring = "";
		int i = 1;
		UserRequest n = trace.get(0);
		UserRequest p = n;
		n = trace.get(i);
		Direction d = UtilityFunctions.getDirection(p,n);
		if(d != null) {
			dirstring += d.getWord();
		}
		i++;
		
		while(i < trace.size()) {
			p = n;
			n = trace.get(i);
			d = UtilityFunctions.getDirection(p,n);
			if(d != null) {
				dirstring += " " + d.getWord();
			}
			i++;
		}
		return dirstring;
	}
	
	public void finishTraining() {
		System.out.println("len: "+this.len+", sentences: "+sentences.size());
		
		// this library assumes ngrams of order 3 or higher
		lm = LmReadersExtension.readKneserNeyLmFromStrings(sentences, wordIndexer, Math.max(3,this.len-1));
	}
	
	public void train(List<UserRequest> trace) {
		sentences.add(buildDirectionStringFromString(trace));
	}
	
	public static void testing() {
		List<String> sentences = new ArrayList<String>();
		sentences.add("testing this out");
		sentences.add("testing sentence building");
		sentences.add("this sentence building testing");
		sentences.add("this sentence is testing");
		sentences.add("this sentence building");
		sentences.add("sentence testing");
		sentences.add("this building testing sentence");
		StringWordIndexer wordIndexer = new StringWordIndexer();
		wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
		wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
		wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);
		ArrayEncodedProbBackoffLm<String> lm = LmReadersExtension.readKneserNeyLmFromStrings(sentences, wordIndexer, 3); // trigrams
		
		int[] bigram = {wordIndexer.getIndexPossiblyUnk("testing"),wordIndexer.getIndexPossiblyUnk("this")};
		System.out.print("indices of: [testing,this]:");
		System.out.print(" "+bigram[0]+", ");
		System.out.println(" "+bigram[1]);
		NgramMap<ProbBackoffPair> x = lm.getNgramMap();
		ProbBackoffPair pbp = x.get(bigram, 0, 2); // end index must be 1 past the index you want to include
		
		// this gives me the very basic Markov stuff
		if(pbp != null) {
			System.out.println("probability of bigram: "+pbp.prob);
		} else {
			System.out.println("bigram doesn't exist!");
		}
		
		// this is what we want!!!! Does the smart ngram stuff
		System.out.println("sentence log probability: "+lm.getLogProb(bigram,0,2));
		
		// the probabilities are stored as log(prob), and are thus negative
		int[] trigram = {bigram[0],bigram[0],bigram[0]};
		System.out.println("trigram sentence log probability: "+lm.getLogProb(trigram,0,3));
	}
	
	public static void main(String[] args) {
		testing();
	}
}
