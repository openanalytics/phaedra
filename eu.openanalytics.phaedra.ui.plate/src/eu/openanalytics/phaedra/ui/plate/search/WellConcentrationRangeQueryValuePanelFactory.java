package eu.openanalytics.phaedra.ui.plate.search;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.Molar;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.ui.search.ConcentrationValueQueryValuePanelUnitExtension;
import eu.openanalytics.phaedra.base.ui.search.RealRangeQueryValuePanelFactory;
import eu.openanalytics.phaedra.model.plate.vo.Well;


public class WellConcentrationRangeQueryValuePanelFactory extends RealRangeQueryValuePanelFactory {
	
	
	private final List<QueryFilter> filters = Arrays.asList(
			new QueryFilter(Well.class, "compoundConcentration", OperatorType.REAL_NUMERIC, Operator.BETWEEN, null) );
	
	
	public WellConcentrationRangeQueryValuePanelFactory() {
	}
	
	
	@Override
	public Collection<QueryFilter> getFilters() {
		return this.filters;
	}
	
	@Override
	protected PanelExtension createExtension(final Panel panel) {
		return new ConcentrationValueQueryValuePanelUnitExtension(panel, Molar);
	}
	
}
