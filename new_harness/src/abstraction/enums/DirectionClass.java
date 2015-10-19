package abstraction.enums;

public enum DirectionClass {
	PAN("P"), OUT("O"), IN("I"), START("S");
	private String val;
	DirectionClass(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
