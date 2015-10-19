package abstraction.enums;

public enum Model {
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
