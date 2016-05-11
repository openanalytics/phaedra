package eu.openanalytics.phaedra.ui.curve.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.search.EnumeratedTextQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveType;
import eu.openanalytics.phaedra.model.curve.vo.CRCurve;


public class CurveTypeQueryValuePanelFactory extends EnumeratedTextQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(CRCurve.class, "type", OperatorType.STRING, Operator.STRING_EQUALS, null))));

	private static List<String> CURVE_TYPES = CollectionUtils.transform(CurveType.values(), c -> c.toString());

	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}

	@Override
	public List<String> getEnumeration() {
		return CURVE_TYPES;
	}

}
