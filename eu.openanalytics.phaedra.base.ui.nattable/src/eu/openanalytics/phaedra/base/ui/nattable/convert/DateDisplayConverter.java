package eu.openanalytics.phaedra.base.ui.nattable.convert;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.nebula.widgets.nattable.data.convert.DefaultDateDisplayConverter;

public class DateDisplayConverter extends DefaultDateDisplayConverter {

	/**
	 * Convert {@link Date} to {@link String} using the default format from {@link SimpleDateFormat}
	 */
	public DateDisplayConverter() {
		super(null, null);
	}

	public DateDisplayConverter(TimeZone timeZone) {
		super(null, timeZone);
	}

	/**
	 * @param dateFormat as specified in {@link SimpleDateFormat}
	 */
	public DateDisplayConverter(String dateFormat) {
		super(dateFormat, null);
	}

	@Override
	public Object canonicalToDisplayValue(Object canonicalValue) {
		if (canonicalValue instanceof Date) {
			return super.canonicalToDisplayValue(canonicalValue);
		}
		return canonicalValue;
	}

}
