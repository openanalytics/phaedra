package eu.openanalytics.phaedra.base.datatype;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import eu.openanalytics.phaedra.base.datatype.description.BasicDataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.format.ConcentrationFormat;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.internal.datatype.Activator;


public class DataTypePrefs {
	
	
	public static final String CONCENTRATION_UNIT_PREFIX = "CONCENTRATION_UNIT_";
	
	public static final String CONCENTRATION_UNIT_DEFAULT = CONCENTRATION_UNIT_PREFIX + "DEFAULT";
	public static final String CONCENTRATION_FORMAT_DEFAULT_DIGITS = "CONCENTRATION_FORMAT_DEFAULT_DIGITS";
	
	private static final String CURVE_PROPERTY_ID = "eu.openanalytics.phaedra.model.curve.vo.Curve";
	public static final String CURVE_CONCENTRATION_UNIT = CONCENTRATION_UNIT_PREFIX + CURVE_PROPERTY_ID;
	
	public static final String TIMESTAMP_FORMAT_DEFAULT = "TIMESTAMP_FORMAT_DEFAULT_PATTERN";
	
	
	public static IPreferenceStore getPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}
	
	
	public static ConcentrationUnit getDefaultConcentrationUnit() {
		final String s = getPreferenceStore().getString(CONCENTRATION_UNIT_DEFAULT);
		return ConcentrationUnit.valueOf(s);
	}
	
	private static ConcentrationUnit getCurveConcentrationUnit() {
		final String s = getPreferenceStore().getString(CURVE_CONCENTRATION_UNIT);
		return ConcentrationUnit.valueOf(s);
	}
	
	public static DataUnitConfig getDefaultDataUnitConfig() {
		final ConcentrationUnit defaultConcentrationUnit = DataTypePrefs.getDefaultConcentrationUnit();
		final ConcentrationUnit curveConcentrationUnit = DataTypePrefs.getCurveConcentrationUnit();
		
		final Map<String, ConcentrationUnit> typeConcentrationUnits = (curveConcentrationUnit != defaultConcentrationUnit) ?
				Collections.singletonMap(CURVE_PROPERTY_ID, curveConcentrationUnit) :
				null;
		return new BasicDataUnitConfig(defaultConcentrationUnit, typeConcentrationUnits);
	}
	
	
	public static int getDefaultConcentrationFormatDigits() {
		return getPreferenceStore().getInt(CONCENTRATION_FORMAT_DEFAULT_DIGITS);
	}
	
	public static DateTimeFormatter getDefaultTimestampFormat() {
		final String value = getPreferenceStore().getString(TIMESTAMP_FORMAT_DEFAULT);
		if (value.startsWith("pattern:")) {
			return DateTimeFormatter.ofPattern(value.substring(8));
		}
		return DateTimeFormatter.ISO_DATE;
	}
	
	public static DataFormatter getDefaultDataFormatter() {
		final ConcentrationUnit defaultConcentrationUnit = DataTypePrefs.getDefaultConcentrationUnit();
		final ConcentrationUnit curveConcentrationUnit = DataTypePrefs.getCurveConcentrationUnit();
		final int concentrationFormatDigits = DataTypePrefs.getDefaultConcentrationFormatDigits();
		final DateTimeFormatter timestampFormat = DataTypePrefs.getDefaultTimestampFormat();
		
		final ConcentrationFormat defaultConcentrationFormat = new ConcentrationFormat(defaultConcentrationUnit, concentrationFormatDigits);
		final Map<String, ConcentrationFormat> typeConcentrationFormats = (curveConcentrationUnit != defaultConcentrationUnit) ?
				Collections.singletonMap(CURVE_PROPERTY_ID, new ConcentrationFormat(curveConcentrationUnit, concentrationFormatDigits)) :
				null;
		return new DataFormatter(defaultConcentrationFormat, typeConcentrationFormats,
				timestampFormat );
	}
	
	
}
