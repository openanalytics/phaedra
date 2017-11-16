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
 * API to execute scripts using any of the supported script engines.
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
	
	/**
	 * Get the {@link ScriptCatalog} for the current environment.
	 */
	public ScriptCatalog getCatalog() {
		return catalog;
	}
	
	/**
	 * Get the IDs of supported script engines.
	 */
	public String[] getEngineIds() {
		return engines.stream().map(e -> e.getId()).toArray(i -> new String[i]);
	}
	
	/**
	 * Get the user-friendly name of the specified script engine.
	 */
	public String getEngineLabel(String id) {
		IScriptEngine engine = getEngine(id);
		return engine == null ? null : engine.getLabel();
	}
	
	/**
	 * Get a list of file type extensions that can be executed
	 * by at least one of the currently supported script engines.
	 */
	public String[] getSupportedFileTypes() {
		String[] types = new String[engines.size()];
		for (int i=0; i<types.length; i++) {
			types[i] = engines.get(i).getFileExtension();
		}
		return types;
	}
	
	/**
	 * Given a script file name, find the ID of a script engine that
	 * can evaluate the script. Returns null if no compatible script
	 * engine was found.
	 */
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
	
	/**
	 * Create a dialog that can assist the user with the creation of a script.
	 * 
	 * @param script The current script body that the user wants to edit in a dialog.
	 * @param engineId The ID of the engine that supports the script language.
	 * @param parentShell The parent shell to create the dialog under.
	 * 
	 * @return A script editing dialog, or null if no appropriate dialog could be created.
	 */
	public Dialog createScriptEditor(StringBuilder script, String engineId, Shell parentShell) {
		IScriptEngine engine = getEngine(engineId);
		if (engine == null) return null;
		return engine.createScriptEditor(parentShell, script);
	}
	
	/*
	 * Script execution methods
	 * ************************
	 */

	/**
	 * See {@link ScriptService#executeScript(String, Map, String)}.
	 */
	public Object executeScript(String body, Map<String, Object> objects) throws ScriptException {
		return executeScript(body, objects, null);
	}
	
	/**
	 * Execute a script.
	 * 
	 * @param body The script to execute.
	 * @param objects Additional top-level objects to pass into the script engine.
	 * @param engineId The ID of the script engine to evaluate the script with, or null to use the default engine.
	 * 
	 * @return The return value of the script, if any.
	 * @throws ScriptException If the script execution fails for any reason.
	 */
	public Object executeScript(String body, Map<String, Object> objects, String engineId) throws ScriptException {
		IScriptEngine engine = getEngine(engineId);
		if (engine == null) throw new ScriptException("Script engine " + engineId + " not found.");
		return engine.eval(body, objects);
	}
	
	/**
	 * Execute a script from a file. An appropriate script engine will be searched for
	 * using the file's extension.
	 * 
	 * @param path The path of the script file to execute.
	 * @param objects Additional top-level objects to pass into the script engine.
	 * 
	 * @return The return value of the script, if any.
	 * @throws ScriptException If the script execution fails for any reason.
	 */
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
