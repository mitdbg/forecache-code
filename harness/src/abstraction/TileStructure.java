package abstraction;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TileStructure implements java.io.Serializable {
	private static final long serialVersionUID = -7588792485497184441L;
	public String[] dimensionLabels; // need to be in dimension order
	public int[][] aggregationWindows; // length = total zoom levels, length of subarray = # dimensions
	public int []tileWidths; // one per dimension
	
	// returns the structure as a json object
	// in case the client needs it to track user stuff
	public String toJson() {
		ObjectMapper o = new ObjectMapper();
		return "";
	}
}
