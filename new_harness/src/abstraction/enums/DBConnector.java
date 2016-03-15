package abstraction.enums;

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
		return this.dbname;
	}
	
	public static DBConnector getConnectorFromString(String dbname) {
		if(dbname.equals("scidb")) {
			return SCIDB;
		} else if(dbname.equals("postgres")) {
			return POSTGRES;
		} else if(dbname.equals("graphulo")) {
			return GRAPHULO;
		}
		return BIGDAWG;
	}
}
