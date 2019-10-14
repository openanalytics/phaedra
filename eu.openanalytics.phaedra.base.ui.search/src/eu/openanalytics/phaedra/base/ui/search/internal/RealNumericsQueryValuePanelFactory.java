package eu.openanalytics.phaedra.base.ui.search.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.RealValuesQueryValuePanelFactory;


public class RealNumericsQueryValuePanelFactory extends RealValuesQueryValuePanelFactory {
	
	
	private final List<QueryFilter> filters = Arrays.asList(
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.IN, null));
	
	
	@Override
	public Collection<QueryFilter> getFilters() {
		return filters;
	}
	
}
