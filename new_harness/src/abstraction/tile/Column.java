package abstraction.tile;

import java.util.List;

public abstract class Column {
	public abstract void add(String string);
	public abstract Object get(int i);
	public abstract Class<?> getColumnType();
	public abstract List<?> getDomain();
	public abstract int getSize();
	public abstract List<?> getValues();
	public abstract boolean isNumeric();
}
