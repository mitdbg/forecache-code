package configurations;

// dummy class, used for setting system configurations
public abstract class Config {
	private DBConnector db;
	
	public Config() {
		this.db = DBConnector.SCIDB;
	}
	
	public Config(DBConnector db) {
		this.db = db;
	}
	
	public DBConnector getDB() {
		return this.db;
	}
	
	public abstract void setConfig();
}
