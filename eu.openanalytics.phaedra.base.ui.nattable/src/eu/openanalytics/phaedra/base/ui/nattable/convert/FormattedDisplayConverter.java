package eu.openanalytics.phaedra.base.ui.nattable.convert;

import static org.eclipse.nebula.widgets.nattable.util.ObjectUtils.isNotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.DefaultComparator;
import org.eclipse.nebula.widgets.nattable.data.convert.NumericDisplayConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import eu.openanalytics.phaedra.base.ui.nattable.misc.INatTableMenuContributor;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;

public class FormattedDisplayConverter extends NumericDisplayConverter implements INatTableMenuContributor {

	private DecimalFormat format;
	private String formatString;
	private boolean fullPrecision;

	/**
	 * Default formatting with full precision.
	 */
	public FormattedDisplayConverter() {
		this(NumberUtils.DEFAULT_DECIMAL_FORMAT, true);
	}

	/**
	 * Default formatting.
	 * @param fullPrecision Use full precision or not
	 */
	public FormattedDisplayConverter(boolean fullPrecision) {
		this(NumberUtils.DEFAULT_DECIMAL_FORMAT, fullPrecision);
	}

	/**
	 * Use given formatting with full precision.
	 * @param formatString The formatting that should be used
	 */
	public FormattedDisplayConverter(String formatString) {
		this(formatString, true);
	}

	/**
	 * Use given formatting.
	 * @param formatString The formatting that should be used
	 * @param fullPrecision Use full precision or not
	 */
	public FormattedDisplayConverter(String formatString, boolean fullPrecision) {
		if (formatString == null) {
			formatString = NumberUtils.DEFAULT_DECIMAL_FORMAT;
		}
		setFormatString(formatString);

		this.fullPrecision = fullPrecision;
	}

	@Override
	public Object canonicalToDisplayValue(Object canonicalValue) {
		try {
			if (isNotNull(canonicalValue)) {
				return format.format(canonicalValue);
			}
			return null;

		} catch (Exception e) {
			return canonicalValue;
		}
	}

	@Override
	protected Object convertToNumericValue(String value) {
		if (fullPrecision) {
			return Double.valueOf(value);
		} else {
			return Float.valueOf(value);
		}
	}

	@Override
	public void fillMenu(NatTable table, Menu menu) {
		MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText("Change formatting");
		menuItem.setEnabled(true);
		menuItem.addListener(SWT.Selection, e-> {
			InputDialog inputDialog = new InputDialog(Display.getDefault().getActiveShell()
					, "Change Formatting"
					, "Change the current format."
					, getFormatString()
					, (newText) -> {
						try {
							NumberUtils.createDecimalFormat(newText);
						} catch (IllegalArgumentException e1) {
							return e1.getMessage();
						}
						return null;
					}
			);
			if (inputDialog.open() == InputDialog.OK) {
				String newFormat = inputDialog.getValue();
				setFormatString(newFormat);
				table.redraw();
			}
		});
	}

	public Comparator<Object> getFilterComparator() {
		return new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				/*
				 * Object 2 is the filter value e.g. for "> 10" the value would be 10.
				 * Object 1 is the raw value, not converted by the display converter.
				 * We convert Object 1 here so that the filter returns what the user expects.
				 * Because of the conversion "= 10" will now return 10.12457 formatted as 10 by the display converter.
				 * If the user wants to do more precise filtering he has to change the column formatting.
				 */
				if (o1 == null) return -1;
				Object o1Converted = canonicalToDisplayValue(o1);
				o1Converted = convertToNumericValue(o1Converted.toString());
				return DefaultComparator.getInstance().compare(o1Converted, o2);
			}
		};
	}

	private String getFormatString() {
		return formatString;
	}

	private void setFormatString(String formatString) {
		this.formatString = formatString;
		this.format = NumberUtils.createDecimalFormat(formatString);

		// Change the ugly NaN symbol (\uFFFD).
		DecimalFormatSymbols decimalFormatSymbols = format.getDecimalFormatSymbols();
		decimalFormatSymbols.setNaN("NaN");
		format.setDecimalFormatSymbols(decimalFormatSymbols);
	}

}