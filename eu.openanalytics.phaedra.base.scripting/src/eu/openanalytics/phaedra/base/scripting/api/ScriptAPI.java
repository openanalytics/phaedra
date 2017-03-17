package eu.openanalytics.phaedra.base.scripting.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.scripting.engine.IScriptEngine;

public class ScriptAPI {

	private IScriptEngine engine;
	private Map<String, Object> api;
	private Map<String, String> apiHelp;
	
	public ScriptAPI(IScriptEngine engine) {
		this.engine = engine;
		this.api = new HashMap<>();
		this.apiHelp = new HashMap<>();
	}
	
	public Object get(String name) {
		return api.get(name);
	}
	
	public String help(String name) {
		String help = apiHelp.get(name);
		if (help == null || help.isEmpty())return "No help available for '" + name + "'";
		else return help;
	}
	
	public void list() {
		String[] names = api.keySet().toArray(new String[api.size()]);
		Arrays.sort(names);
		engine.getConsole().print(Arrays.toString(names));
	}
	
	public void register(String name, Object service, String help) {
		api.put(name, service);
		if (help != null && !help.isEmpty()) apiHelp.put(name, help);
	}
}
