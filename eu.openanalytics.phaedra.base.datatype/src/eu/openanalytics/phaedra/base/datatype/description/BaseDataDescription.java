package eu.openanalytics.phaedra.base.datatype.description;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;

public abstract class BaseDataDescription implements DataDescription {
	
	
	private final String name;
	
	
	public BaseDataDescription(final String name) {
		this.name = name;
	}
	
	@Override
	public DataDescription alterTo(final DataUnitConfig dataUnitConfig) {
		return this;
	}
	
	
	@Override
	public final String getName() {
		return this.name;
	}
	
	
	@Override
	public String convertNameTo(final String name, final DataUnitConfig dataUnitConfig) {
		return name;
	}
	
	@Override
	public IConverter getDataConverterTo(final DataUnitConfig dataUnitConfig) {
		return null;
	}
	
	
	@Override
	public int hashCode() {
		int hash = getClass().hashCode();
		hash = 31 * hash + this.name.hashCode();
		return hash;
	}
	
	@Override
	public boolean equalsType(final DataDescription other) {
		return (getClass() == other.getClass()
				&& getDataType() == other.getDataType()
				&& getContentType() == other.getContentType() );
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof DataDescription) {
			final DataDescription other = (DataDescription)obj;
			return (equalsType(other) && this.name.equals(other.getName()));
		}
		return false;
	}
	
	
	@Override
	public String toString() {
		return String.format("%1$s : %2$s %3$s", this.name, getDataType(), getContentType());
	}
	
}
