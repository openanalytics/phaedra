package eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy;

import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByObject;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByTreeFormat;

/**
 * The TreeList.Format that is used by the TreeList that is created and used by
 * the GroupByDataLayer. Note that the TreeList created by the GroupByDataLayer
 * is generic for Object because the groupBy functionality will add
 * GroupByObjects to the path for creating the grouping.
 *
 * @param <T>
 *            The type of the base objects carried in the TreeList.
 */
public class ExtendedGroupByTreeFormat<T> extends GroupByTreeFormat<T> {

    /**
     * The GroupByModel that carries the information about the groupBy states.
     */
    private final GroupByModel model;
    /**
     * The IColumnAccessor that is used to get the column value for the columns
     * that are grouped by. Needed for compare operations and creating the path
     * in the tree.
     */
    private final IColumnAccessor<T> columnAccessor;

    /**
     *
     * @param model
     *            The GroupByModel that carries the information about the
     *            groupBy states.
     * @param columnAccessor
     *            The IColumnAccessor that is used to get the column value for
     *            the columns that are grouped by. Needed for compare operations
     *            and creating the path in the tree.
     */
    public ExtendedGroupByTreeFormat(GroupByModel model, IColumnAccessor<T> columnAccessor) {
    	super(model, columnAccessor);
        this.model = model;
        this.columnAccessor = columnAccessor;
    }

	@Override
	public void getPath(List<Object> path, Object element) {
    	List<Integer> groupByColumns = model.getGroupByColumnIndexes();
		if (!groupByColumns.isEmpty()) {
			LinkedHashMap<Integer, Object> descriptor = new LinkedHashMap<Integer, Object>();
			for (int columnIndex : groupByColumns) {
				// Build a unique descriptor for the group
				@SuppressWarnings("unchecked")
				Object columnValue = columnAccessor.getDataValue((T) element, columnIndex);
				if (columnValue == null) columnValue = "<empty>";
				descriptor.put(columnIndex, columnValue);
				GroupByObject groupByObject = getGroupByObject(columnValue, descriptor);
				path.add(groupByObject);
			}
		}
		path.add(element);
    }

}
