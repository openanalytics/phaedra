package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class EntityIdDescription extends BaseDataDescription {
	
	
	private final Class<?> referredEntityType;
	
	
	public EntityIdDescription(final String name, final Class<?> entityType,
			final Class<?> referredEntityType) {
		super(name, entityType);
		this.referredEntityType = referredEntityType;
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.Integer;
	}
	
	@Override
	public final ContentType getContentType() {
		return ContentType.EntityId;
	}
	
	public final Class<?> getReferredEntityType() {
		return this.referredEntityType;
	}
	
	
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 31 * hash + this.referredEntityType.hashCode();
		return hash;
	}
	
	@Override
	public boolean equalsType(final DataDescription other) {
		return (super.equalsType(other)
				&& this.referredEntityType == ((EntityIdDescription)other).referredEntityType );
	}
	
	@Override
	public String toString() {
		return super.toString() + " (" + this.referredEntityType.getSimpleName() + ")";
	}
	
}
