package eu.openanalytics.phaedra.base.util.misc;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class StringUtils {

	private static final Pattern NUMERIC_SPLITTER = Pattern.compile("([\\-\\+]?\\d+(\\.\\d+)?|\\D+)");
	private static final Pattern EMAIL_VALIDATOR = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

	public static String trim(String string, int maxLength) {
		// E.g. "Dodecahedron with maxLength 7 becomes "Dode..."
		if (maxLength < 4) return string.substring(0, maxLength);
		if (string.length() <= maxLength) return string;
		return string.substring(0,maxLength-3) + "...";
	}

	public static <E> String createSeparatedString(List<E> items, Function<E, String> mapper, String separator) {
		return createSeparatedString(items.stream().map(mapper).toArray(i->new String[i]), separator);
	}
	
	public static String createSeparatedString(String[] items, String separator) {
		return createSeparatedString(items, separator, true);
	}

	public static String createSeparatedString(String[] items, String separator, boolean whitespace) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<items.length; i++) {
			sb.append(items[i]);
			if (i<items.length-1) sb.append(whitespace ? separator + " " : separator);
		}
		return sb.toString();
	}

	public static <E extends Enum<E>> String[] getEnumNames(E[] enums) {
		String[] names = new String[enums.length];
		for (int i=0; i<names.length; i++) {
			names[i] = enums[i].name();
		}
		return names;
	}

	/**
	 * Returns the extent of the given text.
	 * Note: This method does not dispose of the given Font.
	 * @param text The string to be measured
	 * @param height The available height
	 * @param width The available width
	 * @param usedFont The font used to draw the String
	 * @return
	 */
	public static Point calculateTextSize(String text, int height, int width, Font usedFont) {
		Image tempImg = new Image(null, width, height);
		GC gc = new GC(tempImg);
		gc.setFont(usedFont);
		Point textSize = gc.stringExtent(text);
		// Since stringExtent does not take \n into account, do it ourselves.
		textSize.y *= text.split("\n").length;
		tempImg.dispose();
		gc.dispose();

		return textSize;
	}

	public static boolean isValidEmail(String email) {
		if (email == null) return false;
		Matcher matcher = EMAIL_VALIDATOR.matcher(email);
		return matcher.matches();
	}

	public static String getStackTrace(Throwable t) {
		if (t == null) return "";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream stack = new PrintStream(out, true);
		t.printStackTrace(stack);
		return out.toString();
	}

	public static String getStackTrace(Throwable t, int maxSize) {
		String stackTrace = getStackTrace(t);
		if (stackTrace.length() > maxSize) {
			stackTrace = stackTrace.substring(0, maxSize-3) + "...";
		}
		return stackTrace;
	}

	public static String nonNull(String s) {
		return (s == null) ? "" : s;
	}

	public static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}

	/**
	 * <p>Return given string in proper case.</p>
	 *
	 * <pre>
	 * Example:
	 * 	HELLO		->	Hello
	 * 	world		->	World
	 * 	hEllOWoRlD	->	Helloworld
	 * </pre>
	 *
	 * @param string
	 * @return
	 */
	public static String getProperCase(String string) {
		return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
	}

	/**
	 * Compare Strings containing numbers.
	 * Useful for sorting e.g. Well Positions (A9 comes before A10)
	 *
	 * @param s1 String 1
	 * @param s2 String 2
	 * @return
	 */
	public static int compareToNumericStrings(String s1, String s2) {
		// We split each string as runs of number/non-number strings
		List<String> sa1 = split(s1);
		List<String> sa2 = split(s2);
		// Nothing or different structure
		if (sa1.size() == 0 || sa1.size() != sa2.size()) {
			// Just compare the original strings
			return s1.compareTo(s2);
		}
		int i = 0;
		String si1 = "";
		String si2 = "";
		// Compare beginning of string
		for (; i < sa1.size(); i++) {
			si1 = sa1.get(i);
			si2 = sa2.get(i);
			// Until we find a difference
			if (!si1.equals(si2)) break;
		}
		// No difference found? Same strings!
		if (i == sa1.size()) return 0;

		// Try to convert the different run of characters to number
		double val1, val2;
		try {
			val1 = Double.parseDouble(si1);
			val2 = Double.parseDouble(si2);
		} catch (NumberFormatException e) {
			// Strings differ on a non-number
			return s1.compareTo(s2);
		}

		// Compare remainder of string
		for (i++; i < sa1.size(); i++) {
			si1 = sa1.get(i);
			si2 = sa2.get(i);
			if (!si1.equals(si2)) {
				// Strings differ
				return s1.compareTo(s2);
			}
		}

		// Here, the strings differ only on a number
		return val1 < val2 ? -1 : 1;
	}

	private static List<String> split(String s) {
		List<String> r = new ArrayList<>();
		Matcher matcher = NUMERIC_SPLITTER.matcher(s);
		while (matcher.find()) {
			String m = matcher.group(1);
			r.add(m);
		}
		return r;
	}

}