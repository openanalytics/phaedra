package eu.openanalytics.phaedra.base.ui.theme;

import org.eclipse.swt.graphics.Color;


public class ColorDefinition extends ThemeDefinition {
	
	
	public ColorDefinition(final String id, final String name) {
		super(id, name);
	}
	
	public Color getColor() {
		return PhaedraThemes.getColorRegistry().get(getId());
	}
	
}
