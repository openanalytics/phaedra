package eu.openanalytics.phaedra.base.internal.datatype;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.LogMolar;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;


public class PreferenceInitializer extends AbstractPreferenceInitializer {
	
	
	public PreferenceInitializer() {
	}
	
	
	@Override
	public void initializeDefaultPreferences() {
		final IPreferenceStore store = DataTypePrefs.getPreferenceStore();
		
		store.setDefault(DataTypePrefs.CONCENTRATION_UNIT_DEFAULT, LogMolar.name());
		store.setDefault(DataTypePrefs.CONCENTRATION_FORMAT_DEFAULT_DIGITS, 3);
		store.setDefault(DataTypePrefs.CURVE_CONCENTRATION_UNIT, LogMolar.name());
	}
	
}
