package eu.openanalytics.phaedra.ui.plate.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.LabelProvider;

import com.google.common.base.CaseFormat;

import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.search.EnumeratedIntegerQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;


public class CompoundValidationStatusQueryValuePanelFactory extends EnumeratedIntegerQueryValuePanelFactory {
	private static Set<QueryFilter> FILTERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			new QueryFilter(Compound.class, "validationStatus", OperatorType.NATURAL_NUMERIC, Operator.EQUALS, null))));

	private static List<Integer> VALIDATION_STATUSES = CollectionUtils.transform(CompoundValidationStatus.values(), cvs -> cvs.getCode());

	@Override
	public Set<QueryFilter> getFilters() {
		return FILTERS;
	}

	@Override
	public List<Integer> getEnumeration() {
		return VALIDATION_STATUSES;
	}

	@Override
	public LabelProvider getLabelProvider() {
		return new LabelProvider() {
			@Override
			public String getText(Object element) {
				String upperCase = CompoundValidationStatus.getByCode((Integer) element).toString();
				return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, upperCase) + " (" + element + ")";
			}
		};
	}
}
