package eu.openanalytics.phaedra.base.scripting.engine;

import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.console.InteractiveConsole;
import eu.openanalytics.phaedra.base.scripting.Activator;

public interface IScriptEngine {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".scriptEngine";
	public final static String ATTR_CLASS = "class";
	public final static String ATTR_ID = "id";
	public final static String ATTR_LABEL = "label";
	public final static String ATTR_IS_DEFAULT = "isDefault";
	public final static String ATTR_FILE_EXT = "fileExtension";
	
	public String getId();

	public String getLabel();
	
	public void initialize() throws ScriptException;
	
	public void registerAPI(String name, Object value, String help);
	
	public Object eval(String script) throws ScriptException;

	public Object eval(String script, Map<String, Object> objects) throws ScriptException;
	
	public Object evalFile(String filePath) throws ScriptException;
	
	public Object evalFile(String filePath, Map<String, Object> objects) throws ScriptException;
	
	public InteractiveConsole getConsole();
	
	public boolean isDefault();
	
	public String getFileExtension();
	
	public Dialog createScriptEditor(Shell parentShell, StringBuilder script);
}
