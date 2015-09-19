package abstraction.util;

/*
 * IN1 IN2
 * IN0 IN3
 */
public enum Direction {
	UP("U","up"), DOWN("D","down"), LEFT("L","left"), RIGHT("R","right"), OUT("O","out"),
	IN0("0","in0"), IN1("1","in1"), IN2("2","in2"), IN3("3","in3");
	private String val;
	private String word;
	Direction(String val, String word) {
		this.val = val;
		this.word = word;
	}
	@Override
	public String toString() {
		return val;
	}
	
	public String getWord() {
		return word;
	}
	
	public String getVal() {
		return this.val;
	}
}