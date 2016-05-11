package eu.openanalytics.phaedra.base.ui.nattable.summaryrow;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DummyBodyDataProvider;
import org.eclipse.nebula.widgets.nattable.summaryrow.ISummaryProvider;

public class StatsSummaryProvider extends AbstractStatsSummaryProvider implements ISummaryProvider {

	public static final String SUMMARY_DATAPROVIDER = "SUMMARY_DATAPROVIDER";

    private final IDataProvider dataProvider;

    public StatsSummaryProvider(NatTable table) {
    	Object object = table.getData(SUMMARY_DATAPROVIDER);
    	if (object instanceof IDataProvider) {
    		this.dataProvider = (IDataProvider) object;
    	} else {
    		this.dataProvider = new DummyBodyDataProvider(0, 0);
    	}
    }

    @Override
    public Object summarize(int columnIndex) {
    	List<Double> values = getNumericValues(columnIndex);
    	return summarize(values);
    }

	private List<Double> getNumericValues(int columnIndex) {
		List<Double> values = new ArrayList<>();

		int rowCount = dataProvider.getRowCount();
    	for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
    		Object dataValue = dataProvider.getDataValue(columnIndex, rowIndex);
    		if (dataValue instanceof Number) {
    			values.add(((Number) dataValue).doubleValue());
    		}
        }
		return values;
	}

}