package eu.openanalytics.phaedra.ui.subwell.preferences;



import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.ui.subwell.Activator;

public class Prefs extends AbstractPreferenceInitializer {

	public final static String CLASSIFICATION_SYMBOL_FILL = "CLASSIFICATION_SYMBOL_FILL";
	public final static String CLASSIFICATION_SYMBOL_LINE_WIDTH = "CLASSIFICATION_SYMBOL_LINE_WIDTH";
	public final static String CLASSIFICATION_SYMBOL_SIZE = "CLASSIFICATION_SYMBOL_SIZE";
	public final static String CLASSIFICATION_SYMBOL_OPACITY = "CLASSIFICATION_SYMBOL_OPACITY";
	public final static String CLASSIFICATION_SELECTION_LINE_WIDTH = "CLASSIFICATION_SELECTION_LINE_WIDTH";
	public final static String CLASSIFICATION_SELECTION_LINE_STYLE = "CLASSIFICATION_SELECTION_LINE_STYLE";
	public final static String CLASSIFICATION_SELECTION_LINE_OUTER = "CLASSIFICATION_SELECTION_LINE_OUTER";
	public final static String CLASSIFICATION_SELECTION_LINE_COLOR = "CLASSIFICATION_SELECTION_LINE_COLOR";
	public final static String CLASSIFICATION_SELECTION_LINE_DISTANCE = "CLASSIFICATION_SELECTION_LINE_DISTANCE";
	public final static String CLASSIFICATION_SELECTION_OPACITY = "CLASSIFICATION_SELECTION_OPACITY";
	public final static String CLASSIFICATION_SELECTION_SHAPE = "CLASSIFICATION_SELECTION_SHAPE";
	
	
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(CLASSIFICATION_SYMBOL_FILL, false);
		store.setDefault(CLASSIFICATION_SYMBOL_LINE_WIDTH, 2);
		store.setDefault(CLASSIFICATION_SYMBOL_SIZE, 1);
		store.setDefault(CLASSIFICATION_SYMBOL_OPACITY, 255);
		store.setDefault(CLASSIFICATION_SELECTION_LINE_OUTER, true);
		store.setDefault(CLASSIFICATION_SELECTION_LINE_STYLE, SWT.LINE_DOT);
		store.setDefault(CLASSIFICATION_SELECTION_LINE_WIDTH, 2);
		store.setDefault(CLASSIFICATION_SELECTION_LINE_DISTANCE, 2);
		PreferenceConverter.setDefault(store, CLASSIFICATION_SELECTION_LINE_COLOR, new RGB(255,255,0));
		store.setDefault(CLASSIFICATION_SELECTION_OPACITY, 255);
		store.setDefault(CLASSIFICATION_SELECTION_SHAPE, true);
		
	}
	
	
}
