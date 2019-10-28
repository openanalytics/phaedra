package eu.openanalytics.phaedra.base.datatype.util;

import org.eclipse.jface.util.PropertyChangeEvent;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;


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
		if (event.getProperty().startsWith(DataTypePrefs.CONCENTRATION_UNIT_PREFIX)) {
			super.onPreferenceChanged(event);
		}
	}
	
}
