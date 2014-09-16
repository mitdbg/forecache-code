package backend.util;

public enum DirectionClass {
	PAN("P"), OUT("O"), IN("I");
	private String val;
	DirectionClass(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
