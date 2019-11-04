package eu.openanalytics.phaedra.base.datatype.special;

import eu.openanalytics.phaedra.base.datatype.description.CensoredValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


public class ConcentrationLogPNamedCensoredValueDescription extends ConcentrationLogPNamedValueDescription
		implements CensoredValueDescription {
	
	
	private final String censorName;
	
	
	public ConcentrationLogPNamedCensoredValueDescription(final String name, final Class<?> entityType,
			final ConcentrationUnit unit, final String censorName) {
		super(name, entityType, unit);
		this.censorName = censorName;
	}
	
	@Override
	public ConcentrationLogPNamedCensoredValueDescription alterTo(final DataUnitConfig dataUnitConfig) {
		final ConcentrationUnit toUnit = dataUnitConfig.getConcentrationUnit(this);
		if (toUnit == getConcentrationUnit()) {
			return this;
		}
		return new ConcentrationLogPNamedCensoredValueDescription(
				convertNameTo(getName(), toUnit), getEntityType(),
				toUnit, convertNameTo(getCensorName(), toUnit) );
	}
	
	
	@Override
	public String getCensorName() {
		return this.censorName;
	}
	
	
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 31 * hash + this.censorName.hashCode();
		return hash;
	}
	
	@Override
	public boolean equalsType(final DataDescription other) {
		return (super.equalsType(other)
				&& this.censorName.equals(((CensoredValueDescription)other).getCensorName()) );
	}
	
}
