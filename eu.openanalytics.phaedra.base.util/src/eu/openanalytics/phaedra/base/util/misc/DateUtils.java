package eu.openanalytics.phaedra.base.util.misc;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

	/**
	 * Checks if two dates are on the same day.
	 */
	public static boolean isSameDay(Date date1, Date date2) {
		if (date1 == null || date2 == null) {
			throw new IllegalArgumentException("The dates must not be null");
		}
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		return isSameDay(cal1, cal2);
	}

	/**
	 * Checks if two dates are on the same day.
	 */
	public static boolean isSameDay(Calendar cal1, Calendar cal2) {
		if (cal1 == null || cal2 == null) {
			throw new IllegalArgumentException("The dates must not be null");
		}
		return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
				cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
	}

	/**
	 * Checks if a date is today.
	 */
	public static boolean isToday(Date date) {
		return isSameDay(date, Calendar.getInstance().getTime());
	}
	
	/**
	 * Return the current date formatted with the given pattern.
	 */
	public static String getCurrentDateFormatted(String pattern) {
		Date now = new Date();
		try {
			DateFormat df = new SimpleDateFormat(pattern);
			return df.format(now);
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	/**
	 * Parse a date string which is formatted in the given pattern.
	 */
	public static Date parseDate(String date, String pattern) {
		DateFormat fmt = new SimpleDateFormat(pattern);
		try {
			return fmt.parse(date);
		} catch (ParseException | IllegalArgumentException e) {
			return null;
		}
	}

}