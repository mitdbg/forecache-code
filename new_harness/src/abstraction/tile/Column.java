package abstraction.tile;

import java.util.List;

public abstract class Column implements java.io.Serializable {
	private static final long serialVersionUID = -5580879891349835855L;
	public abstract void add(String string);
	public abstract Object get(int i);
	public abstract Class<?> getColumnType();
	public abstract List<?> getDomain();
	public abstract int getSize();
	public abstract List<?> getValues();
	public abstract boolean isNumeric();
}
