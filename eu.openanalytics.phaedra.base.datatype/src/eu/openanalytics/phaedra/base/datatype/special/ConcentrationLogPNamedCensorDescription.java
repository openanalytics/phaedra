package eu.openanalytics.phaedra.base.datatype.special;

import static eu.openanalytics.phaedra.base.datatype.special.ConcentrationLogPNamedValueDescription.convertName;

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationCensorDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


/**
 * Description for concentration censors with name with 'p' prefix if the unit is LogMolar
 * (e.g. IC50/pIC50 Censor).
 */
public class ConcentrationLogPNamedCensorDescription extends ConcentrationCensorDescription {
	
	
	public ConcentrationLogPNamedCensorDescription(final String name, final Class<?> entityType,
			final ConcentrationUnit unit) {
		super(name, entityType, unit);
	}
	
	@Override
	public ConcentrationLogPNamedCensorDescription alterTo(final DataUnitConfig dataUnitConfig) {
		final ConcentrationUnit toUnit = dataUnitConfig.getConcentrationUnit(this);
		if (toUnit == getConcentrationUnit()) {
			return this;
		}
		return new ConcentrationLogPNamedCensorDescription(
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
