package eu.openanalytics.phaedra.ui.plate.search;

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
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;


public class WellTypeQueryValuePanelFactory extends EnumeratedTextQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(ProtocolClass.class, "highWellTypeCode", OperatorType.STRING, Operator.STRING_EQUALS, null),
			new QueryFilter(ProtocolClass.class, "lowWellTypeCode", OperatorType.STRING, Operator.STRING_EQUALS, null),
			new QueryFilter(Well.class, "wellType", OperatorType.STRING, Operator.STRING_EQUALS, null))));

	private static List<String> WELL_TYPE_CODES = CollectionUtils.transform(ProtocolService.getInstance().getWellTypes(), ProtocolUtils.WELLTYPE_CODES);

	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}

	@Override
	public List<String> getEnumeration() {
		return WELL_TYPE_CODES;
	}

}
