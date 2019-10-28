package eu.openanalytics.phaedra.base.datatype.special;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.LogMolar;

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


/**
 * Description for concentration values with name with 'p' prefix if the unit is LogMolar
 * (e.g. IC50/pIC50).
 */
public class ConcentrationLogPNamedValueDescription extends ConcentrationValueDescription {
	
	
	static String convertName(final String name, final ConcentrationUnit from, final ConcentrationUnit to) {
		final boolean logFrom = (from == LogMolar);
		final boolean logTo = (to == LogMolar);
		if (logFrom && !logTo && name.length() > 1 && (name.charAt(0) == 'p' || name.charAt(0) == 'P')) {
			return name.substring(1);
		}
		if (!logFrom && logTo && name.length() > 0) {
			return 'p' + name;
		}
		return name;
	}
	
	
	public ConcentrationLogPNamedValueDescription(final String name, final Class<?> entityType,
			final ConcentrationUnit unit) {
		super(name, entityType, unit);
	}
	
	@Override
	public ConcentrationLogPNamedValueDescription alterTo(final DataUnitConfig dataUnitConfig) {
		final ConcentrationUnit toUnit = dataUnitConfig.getConcentrationUnit(this);
		if (toUnit == getConcentrationUnit()) {
			return this;
		}
		return new ConcentrationLogPNamedValueDescription(
				convertNameTo(getName(), toUnit), getEntityType(),
				toUnit );
	}
	
	
	@Override
	public String convertNameTo(final String name, final DataUnitConfig dataUnitConfig) {
		return convertNameTo(name, dataUnitConfig.getConcentrationUnit(this));
	}
	
	protected String convertNameTo(final String name, final ConcentrationUnit unit) {
		return convertName(name, getConcentrationUnit(), unit);
	}
	
}
