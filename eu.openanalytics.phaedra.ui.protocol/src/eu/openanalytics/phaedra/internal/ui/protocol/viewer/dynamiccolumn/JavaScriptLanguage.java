package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ScriptLanguage;


public class JavaScriptLanguage extends ScriptLanguage {
	
	
	public static final String ID = "javaScript";
	
	
	public JavaScriptLanguage() {
		super("javaScript", "JavaScript");
	}
	
	
	@Override
	public boolean supportDirectModelObject() {
		return true;
	}
	
}
