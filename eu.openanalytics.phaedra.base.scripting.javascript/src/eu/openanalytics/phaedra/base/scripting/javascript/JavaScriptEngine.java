package eu.openanalytics.phaedra.base.scripting.javascript;

import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.console.InteractiveConsole;
import eu.openanalytics.phaedra.base.scripting.api.ScriptAPI;
import eu.openanalytics.phaedra.base.scripting.engine.BaseScriptEngine;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class JavaScriptEngine extends BaseScriptEngine {

	private Map<String, Object> rootObjects;
	private ScriptEngine engine;
	private Bindings consoleBindings;
	
	@Override
	public void initialize() throws ScriptException {
		rootObjects = new HashMap<String, Object>();

		String label = getLabel();
		if (label == null) label = getId();
		InteractiveConsole console = new InteractiveConsole(label, Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "/icons/js.png")) {
			@Override
			protected String processInput(String input) throws Exception {
				Object output = executeScript(input, null, true);
				if (output == null || output.equals("undefined")) return null;
				return output.toString();
			}
		};
		rootObjects.put("console", console);
		rootObjects.put("API", new ScriptAPI(this));
		setConsole(console);
		
		// Note: this requires access to the ext classloader, e.g. by using -Dosgi.parentClassloader=ext
		// Load the classes dynamically, so an appropriate error message can be given if it fails.
		try {
			ScriptEngineFactory factory = (ScriptEngineFactory) getClass().getClassLoader().loadClass("jdk.nashorn.api.scripting.NashornScriptEngineFactory").newInstance();
			Class<?> filterClass = getClass().getClassLoader().loadClass("jdk.nashorn.api.scripting.ClassFilter");
			Object filter = getClass().getClassLoader().loadClass(getClass().getPackage().getName() + ".EngineClassFilter").newInstance();
			engine = (ScriptEngine) ReflectionUtils.invoke("getScriptEngine", factory, new Object[] {filter}, new Class<?>[] {filterClass});
		} catch (Throwable t) {
			throw new ScriptException("Cannot initialize Nashorn script engine. Please make sure the following property is set: -Dosgi.parentClassloader=ext");
		}
		
		consoleBindings = engine.createBindings();
		getConsole().print("JavaScript Engine: " + engine.getFactory().getEngineName() + " " + engine.getFactory().getEngineVersion()
				+ " (" + engine.getFactory().getLanguageName() + " " + engine.getFactory().getLanguageVersion() + ")");
	}
	
	@Override
	public Object eval(String script, Map<String, Object> objects) throws ScriptException {
		return executeScript(script, objects);
	}
	
	@Override
	public void registerAPI(String name, Object value, String help) {
		ScriptAPI api = (ScriptAPI) rootObjects.get("API");
		if (value instanceof Class<?>) {
			// Reflection is disabled, and static method calls are not allowed on object instances.
			try {
				String className = ((Class<?>) value).getName(); 
				Object staticValue = engine.eval("Java.type(\"" + className + "\")");
				api.register(name, staticValue, help);
			} catch (ScriptException e) {
				throw new RuntimeException("Failed to register static class in script engine", e);
			}
		} else {
			api.register(name, value, help);
		}
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private Object executeScript(String script, Map<String, Object> objects) throws ScriptException {
		return executeScript(script, objects, false);
	}
	
	private Object executeScript(String script, Map<String, Object> objects, boolean console) throws ScriptException {
		// In console, re-use bindings (so the console becomes stateful)
		Bindings bindings = console ? consoleBindings : engine.createBindings();
		for (String key : rootObjects.keySet()) {
			Object obj = rootObjects.get(key);
			bindings.put(key, obj);
		}
		if (objects != null) {
			for (String key : objects.keySet()) {
				Object obj = objects.get(key);
				bindings.put(key, obj);
			}
		}
		try {
			return engine.eval(script, bindings);
		} catch (Exception e) {
			if (e instanceof ScriptException) throw e;
			throw new ScriptException(e);
		}
	}
}