package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.DataType;


public class EntityIdDescription extends BaseDataDescription {
	
	
	private final Class<?> entityType;
	
	
	public EntityIdDescription(final String name, final Class<?> entityType) {
		super(name);
		this.entityType = entityType;
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.Integer;
	}
	
	@Override
	public final ContentType getContentType() {
		return ContentType.EntityId;
	}
	
	public final Class<?> getEntityType() {
		return this.entityType;
	}
	
	
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = 31 * hash + this.entityType.hashCode();
		return hash;
	}
	
	@Override
	public boolean equalsType(final DataDescription other) {
		return (super.equalsType(other)
				&& this.entityType == ((EntityIdDescription)other).entityType );
	}
	
	@Override
	public String toString() {
		return super.toString() + " (" + this.entityType.getSimpleName() + ")";
	}
	
}
