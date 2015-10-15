package abstraction.prediction;

public enum AnalysisPhase {
	FORAGING("Foraging"),NAVIGATION("Navigation"),SENSEMAKING("Sensemaking");
	private String val;
	AnalysisPhase(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
