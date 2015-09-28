package abstraction;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

public class ColumnBasedNiceTile implements java.io.Serializable {
	/**
	 * important for deserialization
	 */
	private static final long serialVersionUID = 543885535684644738L;
	
	public NewTileKey id;
	public List<Column> columns;
	public List<String> attributes = null;
	public List<Class<?>> dataTypes = null;
	
	public ColumnBasedNiceTile() {
		this.id = null;
		this.columns = null;
	}
	
	public ColumnBasedNiceTile(NewTileKey id) {
		this();
		this.id = id;
	}
	
	public ColumnBasedNiceTile(NewTileKey id, List<Column> columns) {
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
	
	protected Column getTypedColumn(Class<?> type) {
		if(type == Boolean.class) return new BooleanColumn();
		else if (type == String.class) return new StringColumn();
		else if(type == Character.class) return new CharacterColumn();
		else if (type == Double.class) return new DoubleColumn();
		else if (type == Float.class) return new FloatColumn();
		else if (type == Integer.class) return new IntegerColumn();
		else if (type == Long.class) return new LongColumn();
		else return new StringColumn();
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
	
	public static class Domain<A extends Comparable<? super A>> {
		protected A low = null;
		protected A high = null;
		
		public void update(A value) {
			if(this.low == null) this.low = value;
			else if (this.low.compareTo(value) > 0) this.low = value;
			
			if(this.high == null) high = value;
			else if (this.high.compareTo(value) < 0) this.high = value;
		}
		
		public List<A> getDomain() {
			if((this.low != null) && (this.high != null)) {
				List<A> domain = new ArrayList<A>();
				domain.add(this.low);
				domain.add(this.high);
				return domain;
			}
			return null;
		}
	}
	
	public static abstract class Column {
		public abstract void add(String string);
		public abstract Object get(int i);
		public abstract Class<?> getColumnType();
		public abstract List<?> getDomain();
		public abstract int getSize();
		public abstract List<?> getValues();
	}
	
	public static class FloatColumn extends Column {
		public List<Float> columnVals;
		public Domain<Float> domain;
		
		public FloatColumn() {
			this.columnVals = new ArrayList<Float>();
			domain = new Domain<Float>();
		}
		
		@Override
		public List<?> getValues() {
			return this.columnVals;
		}
		
		@Override
		public int getSize() {
			return this.columnVals.size();
		}
		
		@Override
		public List<?> getDomain() {
			return this.domain.getDomain();
			
		}
		
		public void add(Float item) {
			this.columnVals.add(item);
			this.domain.update(item);
		}
		
		@Override
		public void add(String value) {
			Float item = Float.parseFloat(value);
			add(item);
		}
		
		@Override
		public Object get(int i) {
			return this.columnVals.get(i);
		}
		
		public Float getTyped(int i) {
			return this.columnVals.get(i);
		}
		
		@Override
		public Class<?> getColumnType() {
			return Float.class;
		}
	}
	
	public static class CharacterColumn extends Column {
		public List<Character> columnVals;
		public Domain<Character> domain;
		
		public CharacterColumn() {
			this.columnVals = new ArrayList<Character>();
			this.domain = new Domain<Character>();
		}
		
		@Override
		public List<?> getValues() {
			return this.columnVals;
		}
		
		@Override
		public int getSize() {
			return this.columnVals.size();
		}
		
		@Override
		public List<?> getDomain() {
			return this.domain.getDomain();
			
		}
		
		public void add(Character item) {
			this.columnVals.add(item);
			this.domain.update(item);
		}
		
		public void add(String value) {
			Character item = new Character(value.charAt(0));
			add(item);
		}
		
		@Override
		public Object get(int i) {
			return this.columnVals.get(i);
		}
		
		public Character getTyped(int i) {
			return this.columnVals.get(i);
		}
		
		@Override
		public Class<?> getColumnType() {
			return Character.class;
		}
	}
	
	public static class BooleanColumn extends Column {
		public List<Boolean> columnVals;
		public Domain<Boolean> domain;
		
		public BooleanColumn() {
			this.columnVals = new ArrayList<Boolean>();
			this.domain = new Domain<Boolean>();
		}
		
		@Override
		public List<?> getValues() {
			return this.columnVals;
		}
		
		@Override
		public int getSize() {
			return this.columnVals.size();
		}
		
		@Override
		public List<?> getDomain() {
			return this.domain.getDomain();
			
		}
		
		public void add(Boolean item) {
			this.columnVals.add(item);
			this.domain.update(item);
		}
		
		public void add(String value) {
			boolean item = Boolean.parseBoolean(value);
			add(item);
		}
		
		@Override
		public Object get(int i) {
			return this.columnVals.get(i);
		}
		
		public Boolean getTyped(int i) {
			return this.columnVals.get(i);
		}
		
		@Override
		public Class<?> getColumnType() {
			return Boolean.class;
		}
	}
	
	public static class DoubleColumn extends Column {
		public List<Double> columnVals;
		public Domain<Double> domain;
		
		public DoubleColumn() {
			this.columnVals = new ArrayList<Double>();
			this.domain = new Domain<Double>();
		}
		
		@Override
		public List<?> getValues() {
			return this.columnVals;
		}
		
		@Override
		public int getSize() {
			return this.columnVals.size();
		}
		
		@Override
		public List<?> getDomain() {
			return this.domain.getDomain();
			
		}
		
		public void add(Double item) {
			this.columnVals.add(item);
			this.domain.update(item);
		}
		
		public void add(String value) {
			Double item = Double.parseDouble(value);
			add(item);
		}
		
		@Override
		public Object get(int i) {
			return this.columnVals.get(i);
		}
		
		public Double getTyped(int i) {
			return this.columnVals.get(i);
		}
		
		@Override
		public Class<?> getColumnType() {
			return Double.class;
		}
	}
	
	public static class IntegerColumn extends Column {
		public List<Integer> columnVals;
		public Domain<Integer> domain;
		
		public IntegerColumn() {
			this.columnVals = new ArrayList<Integer>();
			this.domain = new Domain<Integer>();
		}
		
		@Override
		public List<?> getValues() {
			return this.columnVals;
		}
		
		@Override
		public int getSize() {
			return this.columnVals.size();
		}
		
		@Override
		public List<?> getDomain() {
			return this.domain.getDomain();
			
		}
		
		public void add(Integer item) {
			this.columnVals.add(item);
			this.domain.update(item);
		}
		
		public void add(String value) {
			Integer item = Integer.parseInt(value);
			add(item);
		}
		
		public Integer get(int i) {
			return this.columnVals.get(i);
		}
		
		@Override
		public Class<?> getColumnType() {
			return Integer.class;
		}
	}
	
	public static class LongColumn extends Column {
		public List<Long> columnVals;
		public Domain<Long> domain;
		
		public LongColumn() {
			this.columnVals = new ArrayList<Long>();
			this.domain = new Domain<Long>();
		}
		
		@Override
		public List<?> getValues() {
			return this.columnVals;
		}
		
		@Override
		public int getSize() {
			return this.columnVals.size();
		}
		
		@Override
		public List<?> getDomain() {
			return this.domain.getDomain();
			
		}
		
		public void add(Long item) {
			this.columnVals.add(item);
			this.domain.update(item);
		}
		
		public void add(String value) {
			Long item = Long.parseLong(value);
			add(item);
		}
		
		@Override
		public Object get(int i) {
			return this.columnVals.get(i);
		}
		
		public Long getTyped(int i) {
			return this.columnVals.get(i);
		}
		
		@Override
		public Class<?> getColumnType() {
			return Long.class;
		}
	}
	
	public static class StringColumn extends Column {
		public List<String> columnVals;
		public Domain<String> domain = null;
		
		public StringColumn() {
			this.columnVals = new ArrayList<String>();
			this.domain = new Domain<String>();
		}
		
		@Override
		public List<?> getValues() {
			return this.columnVals;
		}
		
		@Override
		public int getSize() {
			return this.columnVals.size();
		}
		
		@Override
		public List<?> getDomain() {
			return this.domain.getDomain();
			
		}
		
		public void add(String item) {
			this.columnVals.add(item);
			//don't actually calculate the domain
			//this.domain.update(item);
		}
		
		@Override
		public Object get(int i) {
			return this.columnVals.get(i);
		}
		
		public String getTyped(int i) {
			return this.columnVals.get(i);
		}
		
		@Override
		public Class<?> getColumnType() {
			return String.class;
		}
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
		
		ColumnBasedNiceTile t = new ColumnBasedNiceTile();
		t.initializeData(data, dataTypes, attr);
		int dbl_index = t.getIndex("dbl");
		int count = t.getSize();
		List<?> domain = t.getDomain(dbl_index);
		System.out.println(t.attributes.get(dbl_index)+" domain: ["+domain.get(0)+","+domain.get(1)+"]");
		for(int i = 0; i < count; i++) {
			System.out.println(t.attributes.get(0)+": "+t.get(0,i));
			System.out.println(t.attributes.get(1)+": "+t.get(1,i));
		}
	}
}
