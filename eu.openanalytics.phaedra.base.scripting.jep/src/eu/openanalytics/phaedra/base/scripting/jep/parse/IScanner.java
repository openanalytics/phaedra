package eu.openanalytics.phaedra.base.scripting.jep.parse;

import java.util.Map;

import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.scripting.jep.Activator;

public interface IScanner {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".jepScanner";
	public final static String ATTR_CLASS = "class";
	
	public VarReference[] scan(JEPExpression expression, Map<String, Object> context) throws ScriptException;
	
}