package abstraction.util;

import java.util.Iterator;
import java.util.List;

import configurations.DBConnector;

/**
 * @author leibatt
 * This class is used to store basic information about this view
 */
public class View implements java.io.Serializable {
	private static final long serialVersionUID = -3512583197941293678L;
	protected String name; //user-defined view name
	protected String query; //query used to compute the base view to explore
	protected List<String> summaries; // summary operations to compute
	protected DBConnector connectionType; // what kind of connector should we use?
	
	public View(String name, String query, List<String> summaries, DBConnector connectionType) {
		this.name = name;
		this.query = query;
		this.summaries = summaries;
		this.connectionType = connectionType;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getQuery() {
		return this.query;
	}
	
	public DBConnector getConnectionType() {
		return this.connectionType;
	}
	
	public Iterator<String> getSummaryFunctions() {
		return this.summaries.iterator();
	}
	
	@Override
	public String toString() {
		return "View("+this.name+")";
	}

}
