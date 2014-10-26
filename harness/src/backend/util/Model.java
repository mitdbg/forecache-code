package backend.util;

public enum Model {
	MARKOV("markov"),MARKOV1("markov1"),MARKOV2("markov2"),MARKOV3("markov3"),MARKOVN("markovn"),
	MARKOV4("markov4"),
	RANDOM("random"), HOTSPOT("hotspot"), MOMENTUM("momentum"),
	NORMAL("normal"), HISTOGRAM("histogram"), FHISTOGRAM("fhistogram"), NGRAM("ngram"),
	SIFT("sift"),DSIFT("dsift");
	private String val;
	Model(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
