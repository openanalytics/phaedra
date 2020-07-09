package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;


public class ScriptLanguage {
	
	
	private final String id;
	
	private final String label;
	
	
	public ScriptLanguage(final String id, final String label) {
		this.id = id;
		this.label = label;
	}
	
	
	public String getId() {
		return this.id;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	
	public boolean supportDirectModelObject() {
		return false;
	}
	
}
