package eu.openanalytics.phaedra.base.security.model;

/**
 * Represents an object that is "owned" by a user. The owner can make the object private, team, public, ...
 * E.g. a Silo that is owned by a user, but is available to the public with read access.
 */
public interface IOwnedPersonalObject extends IOwnedObject {
	
	public AccessScope getAccessScope();
	
	public String getOwner();

}