package eu.openanalytics.phaedra.base.ui.theme;

import java.util.List;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.ui.PlatformUI;


public class PhaedraThemes {
	
	
	public static final String GREEN_BACKGROUND_INDICATOR_COLOR_ID = "eu.openanalytics.phaedra.base.ui.theme.GREEN_BACKGROUND_INDICATOR_COLOR";
	public static final ColorDefinition GREEN_BACKGROUND_INDICATOR_COLOR = new ColorDefinition(GREEN_BACKGROUND_INDICATOR_COLOR_ID, "Green");
	public static final String RED_BACKGROUND_INDICATOR_COLOR_ID = "eu.openanalytics.phaedra.base.ui.theme.RED_BACKGROUND_INDICATOR_COLOR";
	public static final ColorDefinition RED_BACKGROUND_INDICATOR_COLOR = new ColorDefinition(RED_BACKGROUND_INDICATOR_COLOR_ID, "Red");
	
	
	public static ColorRegistry getColorRegistry() {
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
	}
	
	
	public static <T extends ThemeDefinition> T getDefinition(final List<T> definitions, final String id) {
		for (final T def : definitions) {
			if (def.getId().equals(id)) {
				return def;
			}
		}
		return null;
	}
	
	
	private PhaedraThemes() {
	}
	
}
