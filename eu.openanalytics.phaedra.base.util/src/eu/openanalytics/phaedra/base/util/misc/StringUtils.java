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

/**
 * A collection of utilities related to String manipulation.
 */
public class StringUtils {

	private static final Pattern NUMERIC_SPLITTER = Pattern.compile("([\\-\\+]?\\d+(\\.\\d+)?|\\D+)");
	private static final Pattern EMAIL_VALIDATOR = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

	/**
	 * Trim a String to a specified length.
	 * If the String was longer, "..." will be appended.
	 */
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

	/**
	 * Create a new String by concatenating an array of Strings, optionally
	 * inserting a separator between each.
	 */
	public static String createSeparatedString(String[] items, String separator, boolean whitespace) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<items.length; i++) {
			sb.append(items[i]);
			if (i<items.length-1) sb.append(whitespace ? separator + " " : separator);
		}
		return sb.toString();
	}

	/**
	 * Returns the extent of the given text.
	 * Note: This method does not dispose of the given Font.
	 * 
	 * @param text The string to be measured
	 * @param height The available height
	 * @param width The available width
	 * @param usedFont The font used to draw the String
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

	/**
	 * Checks if a given String is a valid email address.
	 * Only the format is checked, not the existence of the address.
	 */
	public static boolean isValidEmail(String email) {
		if (email == null) return false;
		Matcher matcher = EMAIL_VALIDATOR.matcher(email);
		return matcher.matches();
	}

	/**
	 * Get the full stack trace of a Throwable as a String. 
	 */
	public static String getStackTrace(Throwable t) {
		if (t == null) return "";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream stack = new PrintStream(out, true);
		t.printStackTrace(stack);
		return out.toString();
	}

	/**
	 * Get the stack trace of a Throwable as a String, op to maxSize.
	 * If the stack trace is longer, append "..." at the end.
	 */
	public static String getStackTrace(Throwable t, int maxSize) {
		String stackTrace = getStackTrace(t);
		if (stackTrace.length() > maxSize) {
			stackTrace = stackTrace.substring(0, maxSize-3) + "...";
		}
		return stackTrace;
	}

	/**
	 * Resolve variables in a String by looking for the format '${varName}'.
	 * For each variable found, the resolver is called to translate the variable
	 * reference into a value, which is inserted into the String instead of the reference.
	 * 
	 * Note that the resolution is recursive, so variable values may contain variables
	 * by themselves.
	 */
	public static String resolveVariables(String unresolvedValue, Function<String,String> resolver) {
		if (resolver == null || unresolvedValue == null || unresolvedValue.isEmpty()) return unresolvedValue;
		
		char varStart = '$';
		char varStart2 = '{';
		char varEnd = '}';
		
		StringBuilder resolvedValue = new StringBuilder();
		int len = unresolvedValue.length();
		int position = 0;
		while (position < len) {
			char currentChar = unresolvedValue.charAt(position);
			
			boolean isVarStart = false;
			if (currentChar == varStart && position+1 < len) {
				char nextChar = unresolvedValue.charAt(position+1);
				if (nextChar == varStart2) {
					isVarStart = true;
				}
			}
			
			if (isVarStart) {
				// Consume & replace var
				int varStartPosition = position + 2;
				
				boolean hasVarEnd = true;
				int varEndPosition = varStartPosition;
				while (unresolvedValue.charAt(varEndPosition) != varEnd) {
					varEndPosition++;
					if (varEndPosition == len) {
						hasVarEnd = false;
						break;
					}
				}
				if (hasVarEnd) {
					String varName = unresolvedValue.substring(varStartPosition, varEndPosition);
					String varValue = resolver.apply(varName);
					if (varValue == null) varValue = "" + varStart + varStart2 + varName + varEnd;
					resolvedValue.append(varValue);
					position = varEndPosition;
				} else {
					resolvedValue.append("" + varStart);
				}
			} else {
				resolvedValue.append(currentChar);
			}
			
			position++;
		}
		
		// Keep resolving until there are no more variables to resolve.
		// I.e. a variable might resolve into a String containing more variables.
		String resolvedString = resolvedValue.toString();
		String previousValue = unresolvedValue;
		while (!resolvedString.equals(previousValue)) {
			previousValue = resolvedString;
			resolvedString = resolveVariables(resolvedString, resolver);
		}
		
		return resolvedString;
	}
	
	/**
	 * Get the String, or an empty String if the String is null.
	 */
	public static String nonNull(String s) {
		return (s == null) ? "" : s;
	}

	/**
	 * Check if the given String is null or empty.
	 */
	public static boolean isEmpty(String s) {
		return s == null || s.isEmpty();
	}

	/**
	 * Capitalize the first character of the String, and convert
	 * the rest of the String to lowercase.
	 */
	public static String getProperCase(String string) {
		return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
	}

	/**
	 * Compare Strings containing numbers.
	 * Useful for sorting e.g. Well Positions (A9 comes before A10)
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