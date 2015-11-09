package abstraction.structures;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import abstraction.enums.DBConnector;
import abstraction.structures.TileStructure.TileStructureJson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author leibatt
 * This class is used to store basic information about this view
 */
public class View implements java.io.Serializable {
	private static final long serialVersionUID = -3512583197941293678L;
	protected String name; //user-defined view name
	protected String query; //query used to compute the base view to explore
	protected List<String> summaries; // summary operations to compute for zoom levels
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
	
	// returns this view as a json object
	// in case the client needs it to track user stuff
	public String toJson() {
		return toJson(getViewJson());
		
	}
	
	// populates this view using the given json string
	public boolean fromJson(String jsonstring) {
		ViewJson vjson = null;
		ObjectMapper o = new ObjectMapper();
		try {
			vjson = o.readValue(jsonstring, ViewJson.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(vjson != null) {
			this.connectionType = vjson.connectionType;
			this.name = vjson.name;
			this.query = vjson.query;
			this.summaries = Arrays.asList(vjson.summaries);
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "View("+this.name+")";
	}
	
	/****************** Helper Functions *********************/
	protected ViewJson getViewJson() {
		ViewJson vjson = new ViewJson();
		vjson.connectionType = this.connectionType;
		vjson.name = this.name;
		vjson.query = this.query;
		vjson.summaries = this.summaries.toArray(new String[this.summaries.size()]);
		return vjson;
	}
	
	protected String toJson(ViewJson vjson) {
		ObjectMapper o = new ObjectMapper();
		String returnval = null;
		try {
			returnval = o.writeValueAsString(vjson);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnval;
	}
	
	/****************** Nested Classes *********************/
	public static class ViewJson implements java.io.Serializable {
		private static final long serialVersionUID = 7891278531786309396L;
		protected String name; //user-defined view name
		protected String query; //query used to compute the base view to explore
		protected String[] summaries; // summary operations to compute for zoom levels
		protected DBConnector connectionType; // what kind of connector should we use?
	}

}
