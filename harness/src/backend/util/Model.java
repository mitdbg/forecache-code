package backend.util;

public enum Model {
	MARKOV("markov"), RANDOM("random"), HOTSPOT("hotspot"), MOMENTUM("momentum"),
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
