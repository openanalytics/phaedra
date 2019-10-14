package eu.openanalytics.phaedra.base.datatype;

import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.datatype.format.ConcentrationFormat;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;
import eu.openanalytics.phaedra.base.internal.datatype.Activator;


public class DataTypePrefs {
	
	
	public final static String CONCENTRATION_UNIT_DEFAULT = "CONCENTRATION_UNIT_DEFAULT";
	public final static String CONCENTRATION_FORMAT_DEFAULT_DIGITS = "CONCENTRATION_FORMAT_DEFAULT_DIGITS";
	
	
	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
	
	
	public static ConcentrationUnit getDefaultConcentrationUnit() {
		final String s = getPreferenceStore().getString(CONCENTRATION_UNIT_DEFAULT);
		return ConcentrationUnit.valueOf(s);
	}
	
	public static DataUnitConfig getDefaultDataUnitConfig() {
		return new DataUnitConfig(getDefaultConcentrationUnit());
	}
	
	
	public static int getDefaultConcentrationFormatDigits() {
		return getPreferenceStore().getInt(CONCENTRATION_FORMAT_DEFAULT_DIGITS);
	}
	
	public static DataFormatter getDefaultDataFormatter() {
		return new DataFormatter(
				new ConcentrationFormat(DataTypePrefs.getDefaultConcentrationUnit(), DataTypePrefs.getDefaultConcentrationFormatDigits()) );
	}
	
}
