package eu.openanalytics.phaedra.base.scripting.api;

import java.util.Map;

import eu.openanalytics.phaedra.base.scripting.Activator;

public interface IScriptAPIProvider {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".apiProvider";
	public final static String ATTR_CLASS = "class";
	
	public Map<String, Object> getServices();
	
	public String getHelp(String service);
}
