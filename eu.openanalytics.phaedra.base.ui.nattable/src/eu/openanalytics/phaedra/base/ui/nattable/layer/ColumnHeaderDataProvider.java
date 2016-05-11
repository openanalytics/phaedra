package eu.openanalytics.phaedra.base.ui.nattable.layer;

import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;

public class ColumnHeaderDataProvider<T> implements IDataProvider {

	private IColumnPropertyAccessor<T> accessor;

	public ColumnHeaderDataProvider(IColumnPropertyAccessor<T> accessor) {
		this.accessor = accessor;
	}
	
	@Override
	public Object getDataValue(int columnIndex, int rowIndex) {
		return accessor.getColumnProperty(columnIndex);
	}

	@Override
	public void setDataValue(int columnIndex, int rowIndex, Object newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getColumnCount() {
		return accessor.getColumnCount();
	}

	@Override
	public int getRowCount() {
		return 1;
	}

}