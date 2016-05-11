package eu.openanalytics.phaedra.base.ui.nattable.extension.glazedlists.groupBy.summary;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.summary.IGroupBySummaryProvider;

import eu.openanalytics.phaedra.base.ui.nattable.summaryrow.AbstractStatsSummaryProvider;

public class StatsGroupBySummaryProvider<T> extends AbstractStatsSummaryProvider implements IGroupBySummaryProvider<T> {

    private final IColumnAccessor<T> columnAccessor;

    public StatsGroupBySummaryProvider(IColumnAccessor<T> columnAccessor) {
    	this.columnAccessor = columnAccessor;
    }

    @Override
    public Object summarize(int columnIndex, List<T> children) {
    	List<Double> values = getNumericValues(columnIndex, children);
    	return summarize(values);
    }

	private List<Double> getNumericValues(int columnIndex, List<T> children) {
		List<Double> values = new ArrayList<>();
        for (T child : children) {
            Object dataValue = this.columnAccessor.getDataValue(child, columnIndex);
            if (dataValue instanceof Number) {
            	values.add(((Number) dataValue).doubleValue());
            }
        }
		return values;
	}

}