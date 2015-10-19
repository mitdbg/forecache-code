package abstraction.prediction.directional;

import java.io.File;
import java.io.IOException;
import java.util.List;

import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.io.KneserNeyFileWritingLmReaderCallback;
import edu.berkeley.nlp.lm.io.KneserNeyLmReaderCallback;
import edu.berkeley.nlp.lm.io.LmReaders;
import edu.berkeley.nlp.lm.io.TextReader;


/**
 * @author leibatt
 * This class is an extension to the BerkeleyLm library, for convenient use with my code.
 * It just allows me to build a model directly from a list of sentences, rather than a list of files.
 */
public class LmReadersExtension extends LmReaders {
	
	public static <W> ArrayEncodedProbBackoffLm<W> readKneserNeyLmFromStrings(List<String> sentences, final WordIndexer<W> wordIndexer, final int lmOrder) {
		ArrayEncodedProbBackoffLm<W> returnval = null;
		try{
			File tmpFile = File.createTempFile("__fc_lm_out", ".arpa");
			ConfigOptions opts = new ConfigOptions();
			boolean compress = false;
			createKneserNeyLmFromStrings(sentences,wordIndexer,lmOrder,tmpFile,opts);
			returnval = readArrayEncodedLmFromArpa(tmpFile.getPath(), compress, wordIndexer, opts, lmOrder);
			tmpFile.delete(); // get rid of the file after. THIS IS NECESSARY
		} catch(IOException e) {
			System.out.println("Could not create temporary file");
			e.printStackTrace();
		}
		return returnval;
	}
	
	public static <W> void createKneserNeyLmFromStrings(List<String> sentences, final WordIndexer<W> wordIndexer, final int lmOrder,
			final File arpaOutputFile, final ConfigOptions opts) {
		
			Iterable<String> itsentences = sentences;
			final TextReader<W> reader = new TextReader<W>(itsentences, wordIndexer);
			KneserNeyLmReaderCallback<W> kneserNeyReader = new KneserNeyLmReaderCallback<W>(wordIndexer, lmOrder, opts);
			reader.parse(kneserNeyReader);
			kneserNeyReader.parse(new KneserNeyFileWritingLmReaderCallback<W>(arpaOutputFile, wordIndexer));
	}
}
