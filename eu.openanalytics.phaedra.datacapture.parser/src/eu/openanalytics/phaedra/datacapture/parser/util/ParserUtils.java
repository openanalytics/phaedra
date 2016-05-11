package eu.openanalytics.phaedra.datacapture.parser.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.openanalytics.phaedra.datacapture.parser.ParseException;

public class ParserUtils {
	
	public static String[] toLines(byte[] data) {
		return toLines(data, true);
	}
	
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
	
	public static void doError(String message) throws ParseException {
		throw new ParseException(message);
	}

	public static String find(String[] strings, String regex) {
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		for (String string: strings) {
			if (string == null) continue;
			Matcher matcher = pattern.matcher(string);
			if (matcher.matches()) {
				if (matcher.groupCount() > 0) return matcher.group(1);
				else return string;
			}
		}
		return null;
	}
}