package eu.openanalytics.phaedra.base.security.model;

import java.util.Set;

public class UserContext implements Comparable<UserContext> {

	private String username = "unknown";

	private Set<Group> groups;

	public UserContext(String username, Set<Group> groups) {
		this.username = username;
		this.groups = groups;
	}

	public String getUsername() {
		return username;
	}

	public Group[] getGroups() {
		return groups.toArray(new Group[groups.size()]);
	}

	@Override
	public String toString() {
		return username;
	}

	@Override
	public int compareTo(UserContext other) {
		return username.compareTo(other.username);
	}
}
