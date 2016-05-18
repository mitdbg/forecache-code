package abstraction.tile;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import abstraction.mdstructures.MultiDimTileKey;


public class MultiDimColumnBasedNiceTile implements java.io.Serializable {
	/**
	 * important for deserialization
	 */
	private static final long serialVersionUID = 543885535684644738L;
	
	public MultiDimTileKey id;
	public List<Column> columns;
	public List<String> attributes = null;
	public List<Class<?>> dataTypes = null;
	
	public MultiDimColumnBasedNiceTile() {
		this.id = null;
		this.columns = null;
	}
	
	public MultiDimColumnBasedNiceTile(MultiDimTileKey id) {
		this();
		this.id = id;
	}
	
	public MultiDimColumnBasedNiceTile(MultiDimTileKey id, List<Column> columns) {
		this.id = id;
		this.columns = columns;
	}
	
	public int getIndex(String name) {
		return attributes.indexOf(name);
		/*
		for(int i = 0; i < attributes.size(); i++) {
			if(name.equals(attributes.get(i))) return i;
		}
		return -1;
		*/
	}
	
	// index1 = column, index2 = row
	public Object get(int index1, int index2) {
		Column c = this.columns.get(index1);
		return c.get(index2);
	}
	
	public Column getColumn(int index) {
		return this.columns.get(index);
	}
	
	// total rows
	public int getSize() {
		if(this.columns.size() == 0) return 0;
		Column c = this.columns.get(0);
		return c.getSize();
	}
	
	public List<?> getDomain(int index) {
		if(this.columns.size() > 0) {
			Column c = this.columns.get(index);
			return c.getDomain();
		}
		return null;
	}
	
	public List<?> getColumnValues(int index) {
		if(this.columns.size() > 0) {
			Column c = this.columns.get(index);
			return c.getValues();
		}
		return null;
	}
	
	// made public so TileDecoder can access it
	public Column getTypedColumn(Class<?> type) {
		if(type == Boolean.class) return new BooleanColumn();
		else if (type == String.class) return new StringColumn();
		else if(type == Character.class) return new CharacterColumn();
		else if (type == Double.class) return new DoubleColumn();
		else if (type == Float.class) return new FloatColumn();
		else if (type == Integer.class) return new IntegerColumn();
		else if (type == Long.class) return new LongColumn();
		else return new StringColumn();
	}
	
	protected List<?> getTypedList(Class<?> type) {
		if(type == Boolean.class) return new ArrayList<Boolean>();
		else if (type == String.class) return new ArrayList<String>();
		else if(type == Character.class) return new ArrayList<Character>();
		else if(type == Date.class) return new ArrayList<Date>();
		else if (type == Double.class) return new ArrayList<Double>();
		else if (type == Float.class) return new ArrayList<Float>();
		else if (type == Integer.class) return new ArrayList<Integer>();
		else if (type == Long.class) return new ArrayList<Long>();
		else return new ArrayList<String>();
	}
	
	/*
	 * Initializes the tile assuming that the data is all strings
	 */
	public void initializeDataDefault(List<String> data,List<String> attr) {
		List<Class<?>> dataTypes = new ArrayList<Class<?>>();
		for(int i = 0; i < attr.size(); i++) {
			dataTypes.add(String.class);
		}
		initializeData(data,dataTypes,attr);
	}
	
	// assumes data types have already been added
	public void initializeData(List<String> data, List<String> attr) {
		this.attributes = attr;
		initializeData(data);
	}
	
	public void initializeData(List<String> data, List<Class<?>> dataTypes,List<String> attr) {
		this.attributes = attr;
		this.dataTypes = dataTypes;
		initializeData(data);
	}
	
	public void initializeData(List<String> data) {
		if(this.dataTypes != null) {
			this.columns = new ArrayList<Column>();
			for(int i = 0; i < this.attributes.size(); i++) {
				// need to initialize each column to the appropriate data type
				this.columns.add(getTypedColumn(dataTypes.get(i)));
			}
			
			int col_id = 0;
			int max_col_id = this.attributes.size() - 1;
			for(int i = 0; i < data.size(); i++) {
				Column c = this.columns.get(col_id);
				c.add(data.get(i));
				
				col_id++; // compute the next column id
				if(col_id > max_col_id) {
					col_id = 0;
				}
			}
		}
	}
	
	public String toJson() {
		return toJson(getCbntJson());
	}
	
	/************************ Helper Functions **************************/
	protected MdcbntJson getCbntJson() {
		MdcbntJson temp = new MdcbntJson();
		temp.attributes = attributes.toArray(new String[this.attributes.size()]);
		temp.dataTypes = new String[this.dataTypes.size()];
		for(int i = 0; i < temp.dataTypes.length; i++) {
			temp.dataTypes[i] = this.dataTypes.get(i).getName();
		}
		temp.zoom = this.id.zoom;
		temp.id = this.id.dimIndices;
		int rows = this.getSize();
		int cols = this.columns.size();
		temp.data = new String[cols][rows];
		for(int c = 0; c < cols; c++) { // column-major order
			for(int r = 0; r < rows; r++) {
				// index1 = column, index2 = row
				temp.data[c][r] = this.get(c, r).toString();
			}
		}
		return temp;
	}
	
	protected String toJson(MdcbntJson mdcjson) {
		ObjectMapper o = new ObjectMapper();
		String returnval = null;
		try {
			returnval = o.writeValueAsString(mdcjson);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return returnval;
	}
	
	public static void main(String[] args) {
		List<String> attr = new ArrayList<String>();
		attr.add("str");
		attr.add("dbl");
		
		List<Class<?>> dataTypes = new ArrayList<Class<?>>();
		dataTypes.add(String.class);
		dataTypes.add(Double.class);
		List<String> data = new ArrayList<String>();
		data.add("hello");
		data.add("-2.0");
		data.add("there");
		data.add("3.5");
		data.add("it");
		data.add("0");
		data.add("works!");
		data.add("100.999995");
		
		MultiDimColumnBasedNiceTile t = new MultiDimColumnBasedNiceTile();
		t.initializeData(data, dataTypes, attr);
		t.id = new MultiDimTileKey(new int[]{0,0},new int[]{0});
		int dbl_index = t.getIndex("dbl");
		int count = t.getSize();
		List<?> domain = t.getDomain(dbl_index);
		System.out.println(t.attributes.get(dbl_index)+" domain: ["+domain.get(0)+","+domain.get(1)+"]");
		for(int i = 0; i < count; i++) {
			System.out.println(t.attributes.get(0)+": "+t.get(0,i));
			System.out.println(t.attributes.get(1)+": "+t.get(1,i));
		}
		System.out.println("json:");
		System.out.println(t.toJson());
	}

	/************************** Nested Classes********************************/
	
	public static class MdcbntJson implements java.io.Serializable {
		private static final long serialVersionUID = -7645267735552836298L;
		public int[] id;
		public int[] zoom;
		public String[][] data;
		public String[] attributes;
		public String[] dataTypes;
	}
}
