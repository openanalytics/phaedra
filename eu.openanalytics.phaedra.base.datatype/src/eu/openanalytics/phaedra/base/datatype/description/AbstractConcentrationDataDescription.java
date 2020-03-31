package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


public abstract class AbstractConcentrationDataDescription extends BasicDataDescription implements ConcentrationDataDescription {
	
	
	private final ConcentrationUnit unit;
	
	
	public AbstractConcentrationDataDescription(final String name, final Class<?> entityType,
			final ConcentrationUnit unit) {
		super(name, entityType);
		this.unit = unit;
	}
	
	@Override
	public abstract ConcentrationDataDescription alterTo(final DataUnitConfig dataUnitConfig);
	
	
	@Override
	public ConcentrationUnit getConcentrationUnit() {
		return this.unit;
	}
	
	
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 31 * hash + this.unit.hashCode();
		return hash;
	}
	
	@Override
	public boolean equalsType(final DataDescription other) {
		return (super.equalsType(other)
				&& this.unit == ((AbstractConcentrationDataDescription)other).unit );
	}
	
}
