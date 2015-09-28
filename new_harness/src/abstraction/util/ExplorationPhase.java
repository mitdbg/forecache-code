package abstraction.util;

public enum ExplorationPhase {
	ID("goal-identification"), TRANS("goal-transition"), ANALYZE("goal-analysis");
	private String val;
	ExplorationPhase(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
