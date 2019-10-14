package eu.openanalytics.phaedra.base.ui.search.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.RealValueQueryValuePanelFactory;


public class RealNumericQueryValuePanelFactory extends RealValueQueryValuePanelFactory {
	
	
	private final List<QueryFilter> filters = Arrays.asList(
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.EQUALS, null),
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.GREATER_THAN, null),
			new QueryFilter(null, null, OperatorType.REAL_NUMERIC, Operator.LESS_THAN, null));
	
	
	@Override
	public Collection<QueryFilter> getFilters() {
		return filters;
	}
	
}
