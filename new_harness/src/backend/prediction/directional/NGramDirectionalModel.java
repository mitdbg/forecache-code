package backend.prediction.directional;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.io.ArpaLmReader;
import edu.berkeley.nlp.lm.map.NgramMap;
import edu.berkeley.nlp.lm.values.ProbBackoffPair;

import backend.prediction.BasicModel;
import backend.prediction.DirectionPrediction;
import abstraction.prediction.DefinedTileView;
import abstraction.prediction.SessionMetadata;
import abstraction.util.Direction;
import abstraction.util.Model;
import abstraction.util.NewTileKey;

import abstraction.util.UserRequest;
import abstraction.util.UtilityFunctions;

public class NGramDirectionalModel extends BasicModel {
	protected static List<String> sentences;
	protected static StringWordIndexer wordIndexer;
	protected static ArrayEncodedProbBackoffLm<String> lm;

	public NGramDirectionalModel(int len) {
		super(len);
		sentences = new ArrayList<String>();
		setupNgramModel();
		this.useDistanceCorrection = false;
		this.m = Model.NGRAM;
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
	public List<DirectionPrediction> predictOrder(SessionMetadata md, DefinedTileView dtv, List<NewTileKey> htrace) {
		return super.predictOrder(md,dtv,htrace,true);
	}
	
	@Override
	public Double computeConfidence(SessionMetadata md, DefinedTileView dtv, NewTileKey id, List<NewTileKey> trace) {
		List<NewTileKey> traceCopy = new ArrayList<NewTileKey>();
		traceCopy.addAll(trace);
		NewTileKey prev = traceCopy.get(traceCopy.size() - 1);
		//List<NewTileKey> path = UtilityFunctions.buildPath2(prev, id); // build a path to this key
		List<NewTileKey> path = UtilityFunctions.buildPath(prev, id); // build a path to this key
		return computeConfidenceForPath(md,dtv,path,traceCopy);
	}
	
	@Override
	public Double computeDistance(SessionMetadata md, DefinedTileView dtv, NewTileKey id, List<NewTileKey> trace) {
		return null;
	}
	
	public Double computeConfidenceForPath(SessionMetadata md, DefinedTileView dtv,
			List<NewTileKey> path, List<NewTileKey> traceCopy) {
		List<Direction> dirPath = UtilityFunctions.buildDirectionPath(path);
		if(dirPath.size() == 1) {
			return computeConfidence(md,dtv,dirPath.get(0),traceCopy);
		}
		double prob = 0;
		for(int i = 0; i < dirPath.size(); i++) {
			Direction d = dirPath.get(i);
			prob += computeConfidence(md,dtv,d,traceCopy); // log probabilities
			traceCopy.remove(0);
			traceCopy.add(path.get(i+1));
		}

		return prob;
	}
	
	// average confidence across all directions in the path for this tile
	public Double computeConfidenceForPath2(SessionMetadata md, DefinedTileView dtv,
			List<NewTileKey> path, List<NewTileKey> traceCopy) {
		List<Direction> dirPath = UtilityFunctions.buildDirectionPath(path);
		if(dirPath.size() == 1) {
			return computeConfidence(md,dtv,dirPath.get(0),traceCopy);
		}
		double prob = 0;
		for(int i = 0; i < dirPath.size(); i++) {
			Direction d = dirPath.get(i);
			prob += computeConfidence(md,dtv,d,traceCopy);
			traceCopy.remove(0);
			traceCopy.add(path.get(i+1));
		}
		return prob / dirPath.size(); // just return the average
	}
	
	@Override
	public double computeConfidence(SessionMetadata md, DefinedTileView dtv, Direction d, List<NewTileKey> trace) {
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
	public String buildDirectionStringFromKey(List<NewTileKey> trace) {
		if(trace.size() < 2) {
			return "";
		}
		String dirstring = "";
		int i = 1;
		NewTileKey n = trace.get(0);
		NewTileKey p = n;
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
