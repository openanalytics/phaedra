package eu.openanalytics.phaedra.base.security.model;

/**
 * Represents an object that is "owned" by something or someone.
 * E.g. a Protocol that is owned by a team.
 */
public interface IOwnedObject {

	public String[] getOwners();
}
