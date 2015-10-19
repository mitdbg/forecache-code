package abstraction.prediction.directional;

import java.util.List;

import edu.berkeley.nlp.lm.map.NgramMap;
import edu.berkeley.nlp.lm.values.ProbBackoffPair;

import abstraction.enums.Direction;
import abstraction.structures.DefinedTileView;
import abstraction.structures.NewTileKey;
import abstraction.structures.SessionMetadata;

public class MarkovChainDirectionalModel extends NGramDirectionalModel {
	public MarkovChainDirectionalModel(int len) {
		super(len);
	}
	
	
	@Override
	public double computeConfidence(SessionMetadata md, DefinedTileView dtv,
			Direction d, List<NewTileKey> trace) {
		String p = buildDirectionStringFromKey(trace);
		p += " " + d.getWord();
		//System.out.println("dirstring: \""+p +"\"");
		String[] prefix = p.split(" ");
		int[] ngram = new int[prefix.length];
		for(int i = 0; i < prefix.length; i++) {
			ngram[i] = wordIndexer.getIndexPossiblyUnk(prefix[i]);
		}
		NgramMap<ProbBackoffPair> x = lm.getNgramMap();
		ProbBackoffPair pbp = x.get(ngram, 0, ngram.length); // end index must be 1 past the index you want to include
		
		// this gives me the very basic Markov stuff
		if(pbp != null) {
			return pbp.prob;
		} else {
			return defaultprob;
		}
	}
}
