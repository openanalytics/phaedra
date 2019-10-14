package eu.openanalytics.phaedra.base.datatype.special;

import static eu.openanalytics.phaedra.base.datatype.special.ConcentrationLogPNamedValueDescription.convertName;

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationCensorDescription;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;


/**
 * Description for concentration censors with name with 'p' prefix if the unit is LogMolar
 * (e.g. IC50/pIC50 Censor).
 */
public class ConcentrationLogPNamedCensorDescription extends ConcentrationCensorDescription {
	
	
	public ConcentrationLogPNamedCensorDescription(final String name, final ConcentrationUnit unit) {
		super(name, unit);
	}
	
	@Override
	public ConcentrationLogPNamedCensorDescription alterTo(final DataUnitConfig dataUnitConfig) {
		if (dataUnitConfig.getConcentrationUnit() == getConcentrationUnit()) {
			return this;
		}
		return new ConcentrationLogPNamedCensorDescription(
				convertNameTo(getName(), dataUnitConfig), dataUnitConfig.getConcentrationUnit() );
	}
	
	
	@Override
	public String convertNameTo(final String name, final DataUnitConfig dataUnitConfig) {
		return convertName(name, getConcentrationUnit(), dataUnitConfig.getConcentrationUnit());
	}
	
}
