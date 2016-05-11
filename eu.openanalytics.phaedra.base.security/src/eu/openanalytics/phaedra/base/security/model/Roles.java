package eu.openanalytics.phaedra.base.security.model;

public class Roles {

	public static final String ADMINISTRATOR = "ADMIN";
	public static final String DATA_MANAGER = "MANAGER";
	public static final String USER = "USER";
	public static final String READ_ONLY_USER = "READONLY"; 

	public static boolean extendsRole(String roleA, String roleB) {

		if (roleA.equals(ADMINISTRATOR)) {
			// ADMINISTRATOR extends all roles
			return true;
		}

		if (roleA.equals(DATA_MANAGER)) {
			// DATA_MANAGER extends USER & READ_ONLY_USER
			if (roleB.equals(DATA_MANAGER) || roleB.equals(USER) || roleB.equals(READ_ONLY_USER))
				return true;
			return false;
		}

		if (roleA.equals(USER)) {
			// USER extends READ_ONLY_USER
			if (roleB.equals(USER) || roleB.equals(READ_ONLY_USER))
				return true;
			return false;
		}

		if (roleA.equals(READ_ONLY_USER)) {
			// READ_ONLY_USER extends no other roles
			if (roleB.equals(READ_ONLY_USER))
				return true;
			return false;
		}

		return false;
	}
}
