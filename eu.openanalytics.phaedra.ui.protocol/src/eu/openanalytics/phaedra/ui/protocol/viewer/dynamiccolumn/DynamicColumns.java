package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.description.ConcentrationValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.RealValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.TimestampDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.ui.util.misc.DataLoadStatus;
import eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn.JavaScriptLanguage;


public class DynamicColumns {
	
	
	public static final List<ScriptLanguage> DEFAULT_SCRIPT_LANGUAGES = Arrays.asList(
			new JavaScriptLanguage() );
	
	
	public static final ValueFormat DEFAULT_FORMAT = new ValueFormat("default", "Default");
	
	public static final ValueFormat REAL_CUSTOM_FORMAT = new CustomPatternFormat("Real-Custom", "Custom Format", "0.00") {
		@Override
		public DecimalFormat createFormatter(final String pattern) {
			return new DecimalFormat(pattern);
		}
		@Override
		public DataFormatter createDataFormatter(final FormatConfig formatConfig, final DataFormatter baseDataFormatter) {
			final PatternConfig config = (PatternConfig)formatConfig;
			final DecimalFormat formatter = createFormatter(config.getPattern());
			return new DataFormatter(
						formatter,
						baseDataFormatter.getConcentrationFormat(null),
						null,
						baseDataFormatter.getTimestampFormat(null) );
		}
	};
	
	public static final ValueFormat DATETIME_TIMESTAMP_DEFAULT_FORMAT = new ValueFormat("default", "Default");
	
	public static final ValueFormat DATETIME_TIMESTAMP_CUSTOM_FORMAT = new CustomPatternFormat("DateTime-Custom", "Custom Format", "yyyy-MM-dd HH:mm") {
		@Override
		public DateTimeFormatter createFormatter(final String pattern) {
			return DateTimeFormatter.ofPattern(pattern);
		}
		@Override
		public DataFormatter createDataFormatter(final FormatConfig formatConfig, final DataFormatter baseDataFormatter) {
			final PatternConfig config = (PatternConfig)formatConfig;
			final DateTimeFormatter formatter = createFormatter(config.getPattern());
			return new DataFormatter(
						baseDataFormatter.getConcentrationFormat(null),
						null,
						formatter );
		}
	};
	
	
	public static final ValueDataType DEFAULT_TYPE = new ValueDataType("Default",
			(name, entityType) -> null,
			DEFAULT_FORMAT );
	public static final ValueDataType STRING_TYPE = new ValueDataType(DataType.String.getLabel(),
			(name, entityType) -> new StringValueDescription(name, entityType),
			DEFAULT_FORMAT );
	public static final ValueDataType REAL_TYPE = new ValueDataType(DataType.Real.getLabel(),
			(name, entityType) -> new RealValueDescription(name, entityType),
			DEFAULT_FORMAT, REAL_CUSTOM_FORMAT );
	public static final ValueDataType REAL_CONCENTRATION_Molar_TYPE = new ValueDataType("Concentration in " + ConcentrationUnit.Molar.getLabel(true),
			(name, entityType) -> new ConcentrationValueDescription(name, entityType, ConcentrationUnit.Molar),
			new ValueFormat("default", "Default") {
				@Override
				public FormatEdit createEdit() {
					return new FormatEdit("The value is converted and formatted as configured in the preferences.");
				}
			});
	public static final ValueDataType REAL_CONCENTRATION_LogMolar_TYPE = new ValueDataType("Concentration in " + ConcentrationUnit.LogMolar.getLabel(true),
			(name, entityType) -> new ConcentrationValueDescription(name, entityType, ConcentrationUnit.LogMolar),
			new ValueFormat("default", "Default") {
				@Override
				public FormatEdit createEdit() {
					return new FormatEdit("The value is converted and formatted as configured in the preferences.");
				}
			});
	public static final ValueDataType DATETIME_TIMESTAMP_TYPE = new ValueDataType(DataType.DateTime.getLabel(),
			(name, entityType) -> new TimestampDescription(name, entityType),
			DATETIME_TIMESTAMP_DEFAULT_FORMAT, DATETIME_TIMESTAMP_CUSTOM_FORMAT );
	
	public static final List<ValueDataType> DEFAULT_TYPES = Arrays.asList(
			DEFAULT_TYPE,
			STRING_TYPE,
			REAL_TYPE,
			REAL_CONCENTRATION_Molar_TYPE,
			REAL_CONCENTRATION_LogMolar_TYPE,
			DATETIME_TIMESTAMP_TYPE );
	
	
	public static final ConditionalFormat NONE_CONDITIONAL_FORMAT = new ConditionalFormat("none", "None");
	
	public static final List<ConditionalFormat> DEFAULT_CONDITIONAL_FORMATS = Arrays.asList(
			NONE_CONDITIONAL_FORMAT,
			new ProgressBarFormat() );
	
	
	public static final DataLoadStatus INVALID_CONFIG_STATUS = DataLoadStatus.error("invalid column config");
	public static final DataLoadStatus INVALID_VALUE_STATUS = DataLoadStatus.error("invalid value");
	public static final DataLoadStatus NO_FEATURE_SELECTED_STATUS = new DataLoadStatus(IStatus.INFO, "<no feature selected>");
	
}
