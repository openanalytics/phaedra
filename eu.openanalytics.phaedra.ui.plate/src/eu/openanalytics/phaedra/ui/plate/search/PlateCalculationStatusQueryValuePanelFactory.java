package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.LabelProvider;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.search.EnumeratedIntegerQueryValuePanelFactory;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.validation.ValidationService.PlateCalcStatus;


public class PlateCalculationStatusQueryValuePanelFactory extends EnumeratedIntegerQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(Plate.class, "calculationStatus", OperatorType.NATURAL_NUMERIC, Operator.EQUALS, null))));
	
	private static List<Integer> CALCULATION_STATUSES = Lists.transform(Arrays.asList(PlateCalcStatus.values()), new Function<PlateCalcStatus, Integer>() {
		public Integer apply(PlateCalcStatus plateCalcStatus) {
			return plateCalcStatus.getCode();
		};
	});
	
	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}

	@Override
	public List<Integer> getEnumeration() {
		return CALCULATION_STATUSES;
	}

	@Override
	public LabelProvider getLabelProvider() {
		return new LabelProvider() {
			@Override
			public String getText(Object element) {
				String upperCase = PlateCalcStatus.getByCode((Integer) element).toString();
				return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, upperCase) + " (" + element + ")";
			}
		};
	}
}
	