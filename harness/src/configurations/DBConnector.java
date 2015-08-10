package configurations;

public enum DBConnector {
	SCIDB("scidb"),
	POSTGRES("postgres"),
	GRAPHULO("graphulo"),
	BIGDAWG("bigdawg");
	
	private String dbname;
	DBConnector(String dbname) {
		this.dbname = dbname;
	}
	
	@Override
	public String toString() {
		return "DBConnector("+this.dbname+")";
	}
}
