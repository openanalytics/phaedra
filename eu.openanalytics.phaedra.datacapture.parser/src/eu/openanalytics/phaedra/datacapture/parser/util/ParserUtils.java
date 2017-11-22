package eu.openanalytics.phaedra.datacapture.parser.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of utilities for parsing files.
 */
public class ParserUtils {

	/**
	 * Convert a byte array (containing a parsed text) into an array of lines.
	 * 
	 * @param data The byte array to convert.
	 * @return An array of lines.
	 */
	public static String[] toLines(byte[] data) {
		return toLines(data, true);
	}
	
	/**
	 * Convert a byte array (containing a parsed text) into an array of lines.
	 * 
	 * @param data The byte array to convert.
	 * @param trim True to trim any whitespace from each line.
	 * @return An array of lines.
	 */
	public static String[] toLines(byte[] data, boolean trim) {
		try {
			InputStream input = new ByteArrayInputStream(data);
			BufferedReader in = new BufferedReader(new InputStreamReader(input));
			List<String> lines = new ArrayList<String>();
			String line = null;
			while ((line = in.readLine()) != null) {
				if (trim) line = line.trim();
				lines.add(line);
			}
			return lines.toArray(new String[lines.size()]);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read data", e);
		}
	}

}