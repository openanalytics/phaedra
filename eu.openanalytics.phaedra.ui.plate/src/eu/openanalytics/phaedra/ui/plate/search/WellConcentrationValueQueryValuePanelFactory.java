package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationDataDescription;
import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.ConcentrationValueQueryValuePanelUnitExtension;
import eu.openanalytics.phaedra.base.ui.search.RealValueQueryValuePanelFactory;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Well;


public class WellConcentrationValueQueryValuePanelFactory extends RealValueQueryValuePanelFactory {
	
	
	private final List<QueryFilter> filters = Arrays.asList(
			new QueryFilter(Well.class, "compoundConcentration", OperatorType.REAL_NUMERIC, Operator.EQUALS, null),
			new QueryFilter(Well.class, "compoundConcentration", OperatorType.REAL_NUMERIC, Operator.GREATER_THAN, null),
			new QueryFilter(Well.class, "compoundConcentration", OperatorType.REAL_NUMERIC, Operator.LESS_THAN, null) );
	
	
	public WellConcentrationValueQueryValuePanelFactory() {
	}
	
	
	@Override
	public Collection<QueryFilter> getFilters() {
		return this.filters;
	}
	
	@Override
	protected PanelExtension createExtension(final Panel panel) {
		return new ConcentrationValueQueryValuePanelUnitExtension(panel,
				(ConcentrationDataDescription)WellProperty.Concentration.getDataDescription() );
	}
	
}
