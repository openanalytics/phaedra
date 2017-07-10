package eu.openanalytics.phaedra.base.scripting.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.console.ConsoleManager;
import eu.openanalytics.phaedra.base.console.InteractiveConsole;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.scripting.engine.IScriptEngine;
import eu.openanalytics.phaedra.base.scripting.engine.ScriptEngineFactory;
import eu.openanalytics.phaedra.base.util.io.FileUtils;

/**
 * Provides an API to execute scripts. 
 */
public class ScriptService {

	private static ScriptService instance;
	
	private List<IScriptEngine> engines;
	private IScriptEngine defaultEngine;
	private ScriptCatalog catalog;

	private ScriptService(SecureFileServer fs) {
		// Hidden constructor
		engines = new ArrayList<>();

		String[] engineIds = ScriptEngineFactory.getIds();
		InteractiveConsole[] consoles = new InteractiveConsole[engineIds.length];
		try {
			int i=0;
			for (String id: engineIds) {
				IScriptEngine engine = null;
				try {
					engine = ScriptEngineFactory.createEngine(id);
					engine.initialize();
					if (engine.isDefault()) defaultEngine = engine;
					consoles[i++] = engine.getConsole();
					engines.add(engine);
				} catch (ScriptException e) {
					if (engine.isDefault()) throw new RuntimeException(e);
					else ConsoleManager.getInstance().printErr("Failed to initialize script engine \"" + engine.getId() + "\": " + e.getMessage());
				}
			}
		} finally {
			// Register consoles after instantiating all of them, to ensure correct order in the Console view.
			for (InteractiveConsole c: consoles) {
				if (c != null) ConsoleManager.getInstance().registerConsole(c);
			}
		}

		catalog = new ScriptCatalog(fs);
	}

	public static synchronized ScriptService createInstance(SecureFileServer fs) {
		instance = new ScriptService(fs);
		for (IScriptEngine engine: instance.engines) {
			ScriptEngineFactory.loadEngineServices(engine);
		}
		return instance;
	}
	
	public static ScriptService getInstance() {
		return instance;
	}
	
	/*
	 * **********
	 * Public API
	 * **********
	 */
	
	public ScriptCatalog getCatalog() {
		return catalog;
	}
	
	public String[] getEngineIds() {
		return engines.stream().map(e -> e.getId()).toArray(i -> new String[i]);
	}
	
	public String getEngineLabel(String id) {
		IScriptEngine engine = getEngine(id);
		return engine == null ? null : engine.getLabel();
	}
	
	public String[] getSupportedFileTypes() {
		String[] types = new String[engines.size()];
		for (int i=0; i<types.length; i++) {
			types[i] = engines.get(i).getFileExtension();
		}
		return types;
	}
	
	public String getEngineIdForFile(String fileName) {
		String extension = FileUtils.getExtension(fileName);
		if (extension == null) return null;
		
		// For a given file extension, see if there's an engine capable of executing that file.
		for (IScriptEngine engine: engines) {
			String ext = engine.getFileExtension();
			if (extension.equalsIgnoreCase(ext)) return engine.getId();
		}
		return null;
	}
	
	public Dialog createScriptEditor(StringBuilder script, String engineId, Shell parentShell) {
		IScriptEngine engine = getEngine(engineId);
		if (engine == null) return null;
		return engine.createScriptEditor(parentShell, script);
	}
	
	/*
	 * Script execution methods
	 * ************************
	 */

	public Object executeScript(String body, Map<String, Object> objects) throws ScriptException {
		return executeScript(body, objects, null);
	}
	
	public Object executeScript(String body, Map<String, Object> objects, String engineId) throws ScriptException {
		IScriptEngine engine = getEngine(engineId);
		if (engine == null) throw new ScriptException("Script engine " + engineId + " not found.");
		return engine.eval(body, objects);
	}
	
	public Object executeScriptFile(String path, Map<String, Object> objects) throws ScriptException {
		String engineId = getEngineIdForFile(path);
		IScriptEngine engine = getEngine(engineId);
		if (engine == null) throw new ScriptException("No script engine available to execute file " + path);
		return engine.evalFile(path, objects);
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private IScriptEngine getEngine(String id) {
		if (id == null) return getDefaultEngine();
		for (IScriptEngine engine: engines) {
			if (engine.getId().equalsIgnoreCase(id)) return engine;
		}
		return null;
	}
	
	private IScriptEngine getDefaultEngine() {
		if (defaultEngine == null) return engines.get(0);
		return defaultEngine;
	}
}
