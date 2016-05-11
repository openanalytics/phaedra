package eu.openanalytics.phaedra.base.ui.nattable.misc;

public class NullRichColumnAccessor<T> extends RichColumnAccessor<T> {

	@Override
	public Object getDataValue(T rowObject, int columnIndex) {
		return null;
	}

	@Override
	public int getColumnCount() {
		return 0;
	}

	@Override
	public String getColumnProperty(int columnIndex) {
		return null;
	}

	@Override
	public int getColumnIndex(String propertyName) {
		return 0;
	}

}
