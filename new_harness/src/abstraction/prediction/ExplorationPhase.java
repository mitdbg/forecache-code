package abstraction.prediction;

public enum ExplorationPhase {
	FORAGING("Foraging"),NAVIGATION("Navigation"),SENSEMAKING("Sensemaking");
	private String val;
	ExplorationPhase(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
