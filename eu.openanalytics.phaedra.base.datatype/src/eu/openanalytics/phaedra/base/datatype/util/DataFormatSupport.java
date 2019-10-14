package eu.openanalytics.phaedra.base.datatype.util;

import org.eclipse.jface.util.PropertyChangeEvent;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;


public class DataFormatSupport extends DataConfigSupport<DataFormatter> {
	
	
	public DataFormatSupport(final Runnable onConfigChangedRunnable) {
		super(onConfigChangedRunnable);
	}
	
	public DataFormatSupport() {
		this(null);
	}
	
	
	@Override
	protected DataFormatter createConfig() {
		return DataTypePrefs.getDefaultDataFormatter();
	}
	
	@Override
	protected void onPreferenceChanged(final PropertyChangeEvent event) {
		if (event.getProperty().equals(DataTypePrefs.CONCENTRATION_UNIT_DEFAULT)
				|| event.getProperty().equals(DataTypePrefs.CONCENTRATION_FORMAT_DEFAULT_DIGITS) ) {
			super.onPreferenceChanged(event);
		}
	}
	
}
