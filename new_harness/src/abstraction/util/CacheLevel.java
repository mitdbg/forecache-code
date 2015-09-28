package abstraction.util;

public enum CacheLevel {
	CLIENT("client-mm"), SERVERMM("server-mm"), SERVERDISK("server-disk"),DBMS("dbms");
	private String val;
	CacheLevel(String val) {
		this.val = val;
	}
	@Override
	public String toString() {
		return val;
	}
}
