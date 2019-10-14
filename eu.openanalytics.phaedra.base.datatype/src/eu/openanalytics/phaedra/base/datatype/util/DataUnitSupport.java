package eu.openanalytics.phaedra.base.datatype.util;

import org.eclipse.jface.util.PropertyChangeEvent;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;


public class DataUnitSupport extends DataConfigSupport<DataUnitConfig> {
	
	
	public DataUnitSupport(final Runnable onConfigChangedRunnable) {
		super(onConfigChangedRunnable);
	}
	
	public DataUnitSupport() {
		this(null);
	}
	
	
	@Override
	protected DataUnitConfig createConfig() {
		return DataTypePrefs.getDefaultDataUnitConfig();
	}
	
	@Override
	protected void onPreferenceChanged(final PropertyChangeEvent event) {
		if (event.getProperty().equals(DataTypePrefs.CONCENTRATION_UNIT_DEFAULT)) {
			super.onPreferenceChanged(event);
		}
	}
	
}
