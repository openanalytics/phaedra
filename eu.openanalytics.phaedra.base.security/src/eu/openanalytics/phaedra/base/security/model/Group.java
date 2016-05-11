package eu.openanalytics.phaedra.base.security.model;

public class Group implements Comparable<Group> {

	public static final String GLOBAL_TEAM = "GLOBAL";
	public static final Group GLOBAL_ADMIN_GROUP = new Group(GLOBAL_TEAM, Roles.ADMINISTRATOR);

	private String team;
	private String role;

	public Group(String team, String role) {
		this.team = team;
		this.role = role;
	}

	public String getTeam() {
		return team;
	}

	public String getRole() {
		return role;
	}

	@Override
	public String toString() {
		return team + "_" + role;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((role == null) ? 0 : role.hashCode());
		result = prime * result + ((team == null) ? 0 : team.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Group other = (Group) obj;
		if (role == null) {
			if (other.role != null)
				return false;
		} else if (!role.equals(other.role))
			return false;
		if (team == null) {
			if (other.team != null)
				return false;
		} else if (!team.equals(other.team))
			return false;
		return true;
	}

	public int compareTo(Group group) {
		Group tmp = (Group) group;

		if (this == tmp)
			return 0;

		int comparison = this.role.compareTo(tmp.role);
		if (comparison != 0)
			return comparison;

		comparison = this.team.compareTo(tmp.team);
		if (comparison != 0)
			return comparison;

		return 0;
	}
}
