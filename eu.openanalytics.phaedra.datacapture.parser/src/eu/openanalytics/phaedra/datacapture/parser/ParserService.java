package eu.openanalytics.phaedra.datacapture.parser;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;

/**
 * This service offers a number of parsers that can parse readings (welldata and/or subwelldata) from files.
 * Each parser is identified by a unique ID.
 * A parser can be contributed via the parser extension point (see {@link IParser}) or via a script file (see {@link ParserRegistry}).
 * The parser returns the parsed data in the form of a {@link ParsedModel}.
 */
public class ParserService {
	
	private static ParserService instance;
	
	private ParserRegistry parserRegistry;
	
	private ParserService() {
		// Hidden constructor
		parserRegistry = new ParserRegistry();
	}
	
	public static ParserService getInstance() {
		if (instance == null) instance = new ParserService();
		return instance;
	}

	public ParsedModel parse(String inputFilePath, String parserId) throws ParseException {
		return parse(inputFilePath, parserId, null);
	}
	
	public ParsedModel parse(String inputFilePath, String parserId, Map<String,String> params) throws ParseException {
		try (InputStream input = new FileInputStream(inputFilePath)) {
			return parse(input, parserId, params);
		} catch (IOException e) {
			throw new ParseException("Failed to access file " + inputFilePath);
		}
	}
	
	public ParsedModel parse(byte[] input, String parserId) throws ParseException {
		return parse(new ByteArrayInputStream(input), parserId, null);
	}
	
	public ParsedModel parse(InputStream input, String parserId) throws ParseException {
		return parse(input, parserId, null);
	}
	
	public ParsedModel parse(InputStream input, String parserId, Map<String,String> params) throws ParseException {
		IParser parser = parserRegistry.getParser(parserId);
		if (parser == null) throw new ParseException("No parser found with id " + parserId);
		if (params == null) params = new HashMap<>();
		return parser.parse(input, params);
	}
}
