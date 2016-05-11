package eu.openanalytics.phaedra.base.ui.util.pref;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.util.Activator;
import eu.openanalytics.phaedra.base.ui.util.highlighter.HightlightStyle;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String HIGHTLIGHT_LINE_WIDTH = "HIGHTLIGHT_LINE_WIDTH";
	public final static String HIGHTLIGHT_COLOR_1 = "HIGHTLIGHT_COLOR_1";
	public final static String HIGHTLIGHT_COLOR_2 = "HIGHTLIGHT_COLOR_2";
	public final static String HIGHTLIGHT_STYLE = "HIGHTLIGHT_STYLE";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(HIGHTLIGHT_LINE_WIDTH, 2);
		PreferenceConverter.setDefault(store, HIGHTLIGHT_COLOR_1, Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN).getRGB());
		PreferenceConverter.setDefault(store, HIGHTLIGHT_COLOR_2, Display.getDefault().getSystemColor(SWT.COLOR_BLUE).getRGB());
		store.setDefault(HIGHTLIGHT_STYLE, HightlightStyle.FLASH.getName());
	}

}
