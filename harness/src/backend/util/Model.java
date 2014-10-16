package backend.util;

public enum Model {
	MARKOV("markov"),MARKOV1("markov1"),MARKOV2("markov2"),MARKOV3("markov3"),
	MARKOV4("markov4"),
	RANDOM("random"), HOTSPOT("hotspot"), MOMENTUM("momentum"),
	NORMAL("normal"), HISTOGRAM("histogram"), FHISTOGRAM("fhistogram");
	private String val;
	Model(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
