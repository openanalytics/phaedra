package eu.openanalytics.phaedra.datacapture.parser;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;

/**
 * API to execute parsers on various forms of input (usually files).
 * <p>
 * Parses are expected to return the parsed data in the format of a {@link ParsedModel} object.
 * </p><p>
 * New parsers can be registered via the {@link ParserRegistry}. Note that each parser is expected
 * to have a unique ID.
 * </p>
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

	/**
	 * Parse a file with the given parser.
	 * 
	 * @param inputFilePath The path of the file to parse.
	 * @param parserId The ID of the parser to use.
	 * @return The parsed data.
	 * @throws ParseException If the parse fails for any reason.
	 */
	public ParsedModel parse(String inputFilePath, String parserId) throws ParseException {
		return parse(inputFilePath, parserId, null);
	}
	
	/**
	 * Parse a file with the given parser.
	 * 
	 * @param inputFilePath The path of the file to parse.
	 * @param parserId The ID of the parser to use.
	 * @param params An optional map of parameters to pass to the parser.
	 * @return The parsed data.
	 * @throws ParseException If the parse fails for any reason.
	 */
	public ParsedModel parse(String inputFilePath, String parserId, Map<String,String> params) throws ParseException {
		try (InputStream input = new FileInputStream(inputFilePath)) {
			if (params != null) params.put("inputFilePath", inputFilePath);
			return parse(input, parserId, params);
		} catch (IOException e) {
			throw new ParseException("Failed to access file " + inputFilePath);
		}
	}
	
	/**
	 * Parse an in-memory byte array with the given parser.
	 * 
	 * @param input The byte array containing data to parse.
	 * @param parserId The ID of the parser to use.
	 * @return The parsed data.
	 * @throws ParseException If the parse fails for any reason.
	 */
	public ParsedModel parse(byte[] input, String parserId) throws ParseException {
		return parse(new ByteArrayInputStream(input), parserId, null);
	}

	/**
	 * Parse an InputStream of data with the given parser.
	 * 
	 * @param input The stream of data to parse.
	 * @param parserId The ID of the parser to use.
	 * @return The parsed data.
	 * @throws ParseException If the parse fails for any reason.
	 */
	public ParsedModel parse(InputStream input, String parserId) throws ParseException {
		return parse(input, parserId, null);
	}
	
	/**
	 * Parse an InputStream of data with the given parser.
	 * 
	 * @param input The stream of data to parse.
	 * @param parserId The ID of the parser to use.
	 * @param params An optional map of parameters to pass to the parser.
	 * @return The parsed data.
	 * @throws ParseException If the parse fails for any reason.
	 */
	public ParsedModel parse(InputStream input, String parserId, Map<String,String> params) throws ParseException {
		IParser parser = parserRegistry.getParser(parserId);
		if (parser == null) throw new ParseException("No parser found with id " + parserId);
		if (params == null) params = new HashMap<>();
		return parser.parse(input, params);
	}
}
