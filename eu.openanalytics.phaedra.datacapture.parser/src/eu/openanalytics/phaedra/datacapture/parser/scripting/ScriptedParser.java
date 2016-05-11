package eu.openanalytics.phaedra.datacapture.parser.scripting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.datacapture.parser.AbstractParser;
import eu.openanalytics.phaedra.datacapture.parser.ParseException;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;

public class ScriptedParser extends AbstractParser {

	private String script;
	private String scriptType;
	
	public void setScript(String script) {
		this.script = script;
	}
	
	public void setScriptType(String scriptType) {
		this.scriptType = scriptType;
	}
	
	@Override
	public ParsedModel parse(InputStream input, Map<String,String> params) throws ParseException {
		
		if (script == null || script.isEmpty()) {
			throw new ParseException("Scripted parser error: no script set");
		}
		
		ParsedModel output = new ParsedModel();
		
		Map<String, Object> scriptContext = new HashMap<String, Object>();
		scriptContext.put("model", output);
		scriptContext.put("params", params);
		
		try {
			byte[] dataBuffer = StreamUtils.readAll(input);
			scriptContext.put("data", dataBuffer);
			// For those preferring to read the data as a stream.
			InputStream dataStream = new ByteArrayInputStream(dataBuffer);
			scriptContext.put("dataStream", dataStream);
		} catch (IOException e) {
			throw new ParseException("Script error: failed to read data", e);
		}

		try {
			ScriptService.getInstance().executeScript(script, scriptContext, scriptType);
		} catch (ScriptException e) {
			throw new ParseException("Script error: " + e.getMessage(), e);
		}
		
		return output;
	}

}
