package eu.openanalytics.phaedra.base.util.misc;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of utilities for manipulating numeric values.
 * Methods relating to well numbers use the following terminology:
 * <ul>
 * <li><b>Well Number</b>: this is a number given to each well, starting at 1 for the first well in the upper left corner of the plate.</li>
 * <li><b>Well Position</b>: this is a pair of numbers given to each well, starting at [1,1] for the first well in the upper left corner of the plate.</li>
 * <li><b>Well Coordinate</b>: this is a string representation of the well. Several common formats are supported, such as "A01", "R1C1", "r1_c1", etc</li>
 * </ul>
 */
public class NumberUtils {

	public static final String DEFAULT_DECIMAL_FORMAT = "##.#";
	
	private static final Pattern WELL_COORD_PATTERN = Pattern.compile("([a-zA-Z]+) *-*_* *(\\d+)");
	private static final Pattern WELL_ROW_COL_PATTERN = Pattern.compile("[Rr](\\d+) *-*_* *[Cc](\\d+)");
	private static final DecimalFormat DURATION_FORMAT = new DecimalFormat("00");
	
	private static String Digits = "(\\p{Digit}+)";
	private static String HexDigits = "(\\p{XDigit}+)";
	private static String Exp = "[eE][+-]?"+Digits;
	private static String fpRegex = (
            "[\\x00-\\x20]*"+"[+-]?(" +"NaN|" +"Infinity|" 
            + "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"
            + "(\\.("+Digits+")("+Exp+")?)|"
            + "(((0[xX]"+HexDigits+"(\\.)?)|"
            + "(0[xX]"+HexDigits+"?(\\.)"+HexDigits+")"
            + ")[pP][+-]?"+Digits+"))"+"[fFdD]?))"+"[\\x00-\\x20]*");
	private static Pattern floatPattern = Pattern.compile(fpRegex);

	public static boolean isDouble(String val) {
		if (val == null || val.isEmpty()) return false;
		return (floatPattern.matcher(val).matches());
	}
	
	public static boolean isNumeric(String val) {
		return org.apache.commons.lang3.math.NumberUtils.isNumber(val);
	}
	
	public static boolean isDigit(String val) {
		return org.apache.commons.lang3.math.NumberUtils.isDigits(val);
	}

	public static double roundUp(double val, int decimals) {
		if (Double.isNaN(val) || Double.isInfinite(val))
			return val;
		BigDecimal bd = new BigDecimal(val);
		bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

	public static String round(double value, int decimals) {
		if (Double.isNaN(value)) return "NaN";
		String expr = "#";
		if (decimals > 0) expr += ".";
		for (int i=0;i<decimals;i++) expr += "#";
		DecimalFormat format = createDecimalFormat(expr);
		return format.format(value);
	}
	
	public static int compare(double v1, double v2) {
		if (Double.isNaN(v1) && Double.isNaN(v2)) return 0;
		if (Double.isNaN(v1)) return -1;
		if (Double.isNaN(v2)) return 1;
		if (v1 == Double.NEGATIVE_INFINITY && v2 != Double.NEGATIVE_INFINITY) return -1;
		if (v2 == Double.NEGATIVE_INFINITY && v1 != Double.NEGATIVE_INFINITY) return 1;
		if (v1 == Double.POSITIVE_INFINITY && v2 != Double.POSITIVE_INFINITY) return 1;
		if (v2 == Double.POSITIVE_INFINITY && v1 != Double.POSITIVE_INFINITY) return -1;
		if (v1 < v2) return -1;
		if (v1 > v2) return 1;
		return 0;
	}
	
	public static DecimalFormat createDecimalFormat(String formatString) {
		DecimalFormat format = new DecimalFormat(formatString);
		// Make sure that "." decimal separator is used, regardless of regional setting.
		DecimalFormatSymbols sym = format.getDecimalFormatSymbols();
		sym.setDecimalSeparator('.');
		format.setDecimalFormatSymbols(sym);
		return format;
	}
	
	public static String formatDuration(long duration) {
		int hours = (int)(duration/3600000);
		int minutes = (int)((duration%3600000)/60000);
		int seconds = (int)((duration%60000)/1000);
		return DURATION_FORMAT.format(hours) + ":"
				+ DURATION_FORMAT.format(minutes) + ":"
				+ DURATION_FORMAT.format(seconds);
	}
	
	public static String formatGB(long bytes) {
		double gBytes = (double)bytes / (1024*1024*1024);
		return round(gBytes, 2);
	}
	
	public static String getWellCoordinate(int row, int col) {
		return getWellCoordinate(row, col, null);
	}
	
	/**
	 * Get the well coordinate from a well row nr and column nr.
	 * E.g. 13,24 becomes "M24"
	 */
	public static String getWellCoordinate(int row, int col, String separator) {
		if (separator == null) separator = "";
		return getWellRowLabel(row) + separator + col;
	}
	
	/**
	 * Get the row label for a well row nr.
	 * E.g. 4 becomes "D"
	 */
	public static String getWellRowLabel(int row) {
		String rowString = "";
		if (row <= 26) {
			rowString = "" + (char) (row + 64);
		} else {
			// After row Z, start with AA
			int div = row/26;
			int mod = row%26;
			rowString = "" + (char)(div + 64) + (char)(mod + 64);
		}
		return rowString;
	}
	
	/**
	 * Convert a well coordinate to a well number, starting at 1.
	 * E.g. "C10" with 12 columns per row becomes 34
	 * 
	 * @param coordinate The well coordinate, e.g. "P24" or "R10-C12".
	 * @param colsPerRow The number of columns in the plate, e.g. 12 or 24.
	 */
	public static int getWellNr(String coordinate, int colsPerRow) {
		int row = convertToRowNumber(coordinate);
		int col = convertToColumnNumber(coordinate);
		int value = (row-1)*colsPerRow + col;
		return value;
	}

	/**
	 * Convert a well position to a well number, starting at 1.
	 * E.g. 2,3 with 12 columns per row becomes 34
	 */
	public static int getWellNr(int row, int col, int colsPerRow) {
		int value = (row-1)*colsPerRow + col;
		return value;
	}
	
	/**
	 * Get the well position from a well number.
	 * E.g. wellNr 96 with 12 columns per row becomes [8,12]
	 * 
	 * @param wellNr The well number, starting from 1.
	 * @param colsPerRow The number of columns in the plate, e.g. 12 or 24.
	 */
	public static int[] getWellPosition(int wellNr, int colsPerRow) {
		wellNr--;
		int rowNr = 1 + wellNr / colsPerRow;
		int colNr = 1 + wellNr % colsPerRow;
		return new int[]{rowNr,colNr};
	}
	
	public static int convertToRowNumber(String wellId) {
		Matcher matcher = WELL_COORD_PATTERN.matcher(wellId);
		if (matcher.matches()) {
			String rowString = matcher.group(1);
			int len = rowString.length();
			int row = 0;
			for (int index = 0; index<len; index++) {
				char c = rowString.charAt(index);
				row += (c - 64) * Math.pow(26, (len-index)-1);
			}
			return row;
		} else {
			matcher = WELL_ROW_COL_PATTERN.matcher(wellId);
			if (matcher.matches()) {
				String rowString = matcher.group(1);
				return Integer.valueOf(rowString);
			} else {
				return 0;
			}
		}
	}

	public static int convertToColumnNumber(String wellId) {
		Matcher matcher = WELL_COORD_PATTERN.matcher(wellId);
		if (matcher.matches()) {
			String colString = matcher.group(2);
			int col = Integer.parseInt(colString);
			return col;
		} else {
			matcher = WELL_ROW_COL_PATTERN.matcher(wellId);
			if (matcher.matches()) {
				String colString = matcher.group(2);
				return Integer.valueOf(colString);
			} else {
				return 0;
			}
		}
	}
	
}