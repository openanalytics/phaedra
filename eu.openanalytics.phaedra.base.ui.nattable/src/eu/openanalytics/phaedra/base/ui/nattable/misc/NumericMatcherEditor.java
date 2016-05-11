package eu.openanalytics.phaedra.base.ui.nattable.misc;

import org.eclipse.nebula.widgets.nattable.data.IColumnAccessor;
import org.eclipse.nebula.widgets.nattable.data.convert.IDisplayConverter;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

/**
 * Handles both numeric values (Numbers or Strings containing numbers) and Strings.
 * Numeric values are filtered exactly (5 != 5.123). Strings are filtered using "contains" (case sensitive).
 */
public class NumericMatcherEditor<T> extends AbstractMatcherEditor<T> {

	private int columnIndex;
	private IColumnAccessor<T> columnAccessor;
	private IDisplayConverter displayConverter;
	private String filterText;
	private Double numericFilter;

	public NumericMatcherEditor(int columnIndex, IColumnAccessor<T> columnAccessor, IDisplayConverter displayConverter, String filterText) {
		this.columnIndex = columnIndex;
		this.columnAccessor = columnAccessor;
		this.displayConverter = displayConverter;
		this.filterText = filterText;
		if (displayConverter != null) this.filterText = displayConverter.canonicalToDisplayValue(filterText).toString();
		if (NumberUtils.isDouble(filterText)) numericFilter = Double.parseDouble(filterText);
		fireChanged(new NumericMatcher());
	}

	private class NumericMatcher implements Matcher<T> {
		@Override
		public boolean matches(T arg) {
			Object dataValue = columnAccessor.getDataValue(arg, columnIndex);
			if (displayConverter != null) dataValue = displayConverter.canonicalToDisplayValue(dataValue);
			if (dataValue == null) return false;
			if (numericFilter != null) {
				if (dataValue instanceof Number) return numericFilter.equals(((Number)dataValue).doubleValue());
				else {
					// Note: this prevents "contains" filters on numeric strings such as compound nrs.
					String stringVal = dataValue.toString();
					if (!stringVal.isEmpty()) {
						char c = stringVal.charAt(0);
						if (c == '<' || c == '>' || c == '~') stringVal = stringVal.substring(1);
					}
					if (NumberUtils.isDouble(stringVal)) return numericFilter.equals(Double.parseDouble(stringVal));
				}
			}
			// Default string comparison: "contains" (case sensitive)
			return dataValue.toString().contains(filterText);
		}
	}
}
