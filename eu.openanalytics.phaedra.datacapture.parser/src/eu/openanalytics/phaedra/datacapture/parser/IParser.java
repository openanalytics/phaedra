package eu.openanalytics.phaedra.datacapture.parser;

import java.io.InputStream;
import java.util.Map;

import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;

public interface IParser {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".dataParser";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	
	public String getId();
	
	public ParsedModel parse(InputStream input, Map<String,String> params) throws ParseException;
}
