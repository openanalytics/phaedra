package eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy;

import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByObject;

/**
 * <p>Adds a column called <em>Group By</em> to the end of the columns. This column will contain the Group By column info.
 * It's added to the end to prevent issues regarding custom comparators, display converters, etc...</p>
 *
 * <p>Because it's nicer to have this column as the first column, you can use the ColumnReorderLayer.</p>
 * <p><code>bodyLayerStack.getColumnReorderLayer().reorderColumnPosition(columnAccessor.getColumnCount()-1, 0);</code></p>
 *
 * @param <T> ColumnAccessor type
 */
public class ExtendedGroupByColumnAccessor<T> implements IColumnPropertyAccessor<T> {

	private static final String GROUP_BY = "Group By";

	private final IColumnPropertyAccessor<T> columnAccessor;

	public ExtendedGroupByColumnAccessor(IColumnPropertyAccessor<T> columnAccessor) {
		this.columnAccessor = columnAccessor;
	}

	@Override
	 @SuppressWarnings("unchecked")
	public Object getDataValue(Object rowObject, int columnIndex) {
		if (rowObject instanceof GroupByObject) {
			GroupByObject groupByObject = (GroupByObject) rowObject;
			return groupByObject.getValue();
		} else {
			if (columnIndex < columnAccessor.getColumnCount()) {
				return this.columnAccessor.getDataValue((T) rowObject, columnIndex);
			}
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setDataValue(Object rowObject, int columnIndex, Object newValue) {
		if (rowObject instanceof GroupByObject) {
			// Do nothing.
		} else {
			if (columnIndex < columnAccessor.getColumnCount()) {
				columnAccessor.setDataValue((T) rowObject, columnIndex, newValue);
			}
		}
	}

	@Override
	public int getColumnCount() {
		return columnAccessor.getColumnCount() + 1;
	}

	@Override
	public String getColumnProperty(int columnIndex) {
		if (columnIndex < columnAccessor.getColumnCount()) {
			return columnAccessor.getColumnProperty(columnIndex);
		}
		return GROUP_BY;
	}

	@Override
	public int getColumnIndex(String propertyName) {
		if (propertyName.equalsIgnoreCase(GROUP_BY)) return columnAccessor.getColumnCount();
		return columnAccessor.getColumnIndex(propertyName);
	}

}