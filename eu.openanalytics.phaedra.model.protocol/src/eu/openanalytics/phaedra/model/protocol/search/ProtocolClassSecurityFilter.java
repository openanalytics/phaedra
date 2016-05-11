package eu.openanalytics.phaedra.model.protocol.search;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.search.AbstractSecurityFilter;
import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public class ProtocolClassSecurityFilter extends AbstractSecurityFilter {
	@Override
	public QueryFilter getInternalFilter() {
		List<ProtocolClass> allowedProtocolClasses = ProtocolService.getInstance().getProtocolClasses();

		QueryFilter filter = new QueryFilter();
		filter.setType(ProtocolClass.class);
		filter.setColumnName("id");
		filter.setOperatorType(OperatorType.NATURAL_NUMERIC);
		filter.setOperator(Operator.IN);
		filter.setValue(new ArrayList<>(CollectionUtils.transform(allowedProtocolClasses, pc -> pc.getId())));
		return filter;
	}
}
