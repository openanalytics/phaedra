package eu.openanalytics.phaedra.base.db;

/**
 * A value object is an object that is persistent and represents a domain object.
 * 
 * It has a unique identifier of type long (unique among other instances of the same class),
 * and is typically stored in a database.
 * 
 * It may have a parent IValueObject, which represents a cascading parent-child relationship.
 */
public interface IValueObject {

	public long getId();
	
	public IValueObject getParent();
}
