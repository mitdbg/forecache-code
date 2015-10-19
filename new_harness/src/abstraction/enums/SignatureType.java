package abstraction.enums;

public enum SignatureType {
	SIFT("random"), NORMAL("hotspot"), HISTOGRAM("momentum"),
	FHISTOGRAM("fhistogram"),DSIFT("dsift");
	private String val;
	SignatureType(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
