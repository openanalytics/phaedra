package eu.openanalytics.phaedra.base.ui.theme;


public class ThemeDefinition {
	
	
	private final String id;
	
	private final String name;
	
	
	public ThemeDefinition(final String id, final String name) {
		this.id = id;
		this.name = name;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
}
