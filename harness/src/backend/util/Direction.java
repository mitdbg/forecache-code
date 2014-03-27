package backend.util;

/*
 * IN1 IN2
 * IN0 IN3
 */
public enum Direction {
	UP("U"), DOWN("D"), LEFT("L"), RIGHT("R"), OUT("O"), IN0("0"), IN1("1"), IN2("2"), IN3("3");
	private String val;
	Direction(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}