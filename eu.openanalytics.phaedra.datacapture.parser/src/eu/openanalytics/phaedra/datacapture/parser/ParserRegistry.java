package eu.openanalytics.phaedra.datacapture.parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.parser.scripting.ScriptedParser;

/**
 * This registry contains a listing of all known data parsers.
 * <p>
 * Parsers are primarily used during data capture jobs. Each parser has a unique ID
 * that can be used to invoke the parser (see {@link ParserService}).
 * </p>
 * <p>
 * Two types of parsers are supported:
 * <ul>
 * <li>Java parsers: are written in Java and contributed via the {@link IParser} extension point</li>
 * <li>Scripted parsers: are written in script (e.g. JavaScript) and are contributed by placing them
 * in the <b>/data.parsers</b> folder on the file server. The name of the script file (without its 
 * extension) will serve as the parser's ID.</li>
 * </ul>
 */
public class ParserRegistry {

	private Map<String, IParser> javaParsers;
	
	private final static String SCRIPT_REPO_PATH = "/data.parsers";
	
	public ParserRegistry() {
		loadJavaParsers();
	}
	
	public IParser getParser(String id) {
		IParser parser = null;
		try {
			parser = findScriptedParser(id);
		} catch (IOException e) {
			EclipseLog.error("Failed to load scripted parser with id " + id, e, Activator.getDefault());
		}
		if (parser == null) parser = javaParsers.get(id);
		return parser;
	}
	
	private void loadJavaParsers() {
		javaParsers = new HashMap<String, IParser>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IParser.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				IParser parser = (IParser) el.createExecutableExtension(IParser.ATTR_CLASS);
				javaParsers.put(parser.getId(), parser);
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}
	
	private ScriptedParser findScriptedParser(String id) throws IOException {
		IEnvironment env = Screening.getEnvironment();
		if (env == null) return null;
		
		String[] supportedTypes = ScriptService.getInstance().getSupportedFileTypes();
		for (String type: supportedTypes) {
			String expectedPath = SCRIPT_REPO_PATH + "/" + id + "." + type;
			if (env.getFileServer().exists(expectedPath)) {
				ScriptedParser parser = new ScriptedParser();
				parser.setScript(env.getFileServer().getContentsAsString(expectedPath));
				parser.setScriptType(ScriptService.getInstance().getEngineIdForFile(expectedPath));
				parser.setId(id);
				return parser;
			}
		}

		return null;
	}
}
