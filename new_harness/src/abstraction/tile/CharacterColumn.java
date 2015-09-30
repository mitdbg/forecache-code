package abstraction.tile;

import java.util.ArrayList;
import java.util.List;


public class CharacterColumn extends Column {
	public List<Character> columnVals;
	public Domain<Character> domain;
	
	public CharacterColumn() {
		this.columnVals = new ArrayList<Character>();
		this.domain = new Domain<Character>();
	}
	
	@Override
	public List<Character> getValues() {
		return this.columnVals;
	}
	
	@Override
	public boolean isNumeric() {
		return false;
	}
	
	@Override
	public int getSize() {
		return this.columnVals.size();
	}
	
	@Override
	public List<Character> getDomain() {
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
	public Class<Character> getColumnType() {
		return Character.class;
	}
}
