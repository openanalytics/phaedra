package eu.openanalytics.phaedra.base.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;

import eu.openanalytics.phaedra.base.security.ldap.LDAPSecureLoginHandler;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.security.model.IOwnedObject;
import eu.openanalytics.phaedra.base.security.model.IOwnedPersonalObject;
import eu.openanalytics.phaedra.base.security.model.Roles;
import eu.openanalytics.phaedra.base.security.model.UserContext;
import eu.openanalytics.phaedra.base.security.ui.AccessDialog;
import eu.openanalytics.phaedra.base.security.windows.WindowsLoginHandler;

/**
 * This class takes care of the following security aspects:
 * 
 * <ul>
 * <li>Authenticating users</li>
 * <li>Maintaining information on the currently logged in user</li>
 * <li>Maintaining the list of group memberships for all users</li>
 * <li>Performing security checks (does user X have permission Y on object Z?)</li>
 * </ul>
 */
public class SecurityService {

	private static SecurityService instance;
	
	private String currentUserName;
	private Map<Group, List<String>> securityConfig;

	private ILoginHandler loginHandler;
	private AuthConfig authConfig;

	private SecurityService(AuthConfig authConfig) {
		// Hidden constructor
		this.authConfig = authConfig;
		
		if (authConfig == null) this.loginHandler = new EmbeddedLoginHandler();
		else if (Boolean.valueOf(authConfig.get(AuthConfig.WIN_LOGON))) this.loginHandler = new WindowsLoginHandler(authConfig);
		else this.loginHandler = new LDAPSecureLoginHandler();
	}

	public static synchronized SecurityService createInstance(AuthConfig ldapConfig) {
		instance = new SecurityService(ldapConfig);
		return instance;
	}
	
	public static SecurityService getInstance() {
		return instance;
	}
	
	/*
	 * **********
	 * Public API
	 * **********
	 */
	
	/**
	 * Obtain a login handler that performs user authentication.
	 * 
	 * @return A LoginHandler capable of authenticating users.
	 */
	public ILoginHandler getLoginHandler() {
		return loginHandler;
	}

	//TODO Rename method
	public AuthConfig getLdapConfig() {
		return authConfig;
	}

	public void setSecurityConfig(Map<Group, List<String>> securityConfig) {
		this.securityConfig = securityConfig;
	}

	/**
	 * Set the current user context, defining the person that is currently using the application.
	 */
	public void setCurrentUser(String userName) {
		// To avoid case sensitivity issues, all usernames are lowercase.
		if (userName.contains("\\")) userName = userName.substring(userName.indexOf('\\')+1);
		currentUserName = userName.toLowerCase();
	}

	public String getCurrentUserName() {
		return currentUserName;
	}

	/**
	 * Check if the current user is a global administrator.
	 */
	public boolean isGlobalAdmin() {
		return isGlobalAdmin(currentUserName);
	}

	public boolean isGlobalAdmin(String userName) {
		if (authConfig == null) return true;
		for (Group group : getMemberships(userName)) {
			if (group.equals(Group.GLOBAL_ADMIN_GROUP)) return true;
		}
		return false;
	}

	/*
	 * **************
	 * Permission API
	 * **************
	 */

	public boolean check(String userName, String requiredRole, Object object) {
		return check(userName, requiredRole, object, false, false);
	}

	public boolean check(String requiredRole, Object object) {
		return check(getCurrentUserName(), requiredRole, object, false, false);
	}

	public boolean checkWithException(String requiredRole, Object object) {
		return check(getCurrentUserName(), requiredRole, object, false, true);
	}

	public boolean checkWithDialog(String requiredRole, Object object) {
		return check(getCurrentUserName(), requiredRole, object, true, false);
	}

	/**
	 * Check if the current user has the required role on the specified object.
	 * 
	 * A user has access if following conditions are met:
	 * 
	 * <ol>
	 * <li>The user must be part of the team owning the object.</li>
	 * <li>The user must have a role equal to or higher than the required role
	 * in that team.</li>
	 * </ol>
	 * Global administrators always have unlimited access.
	 * 
	 * <p>
	 * Example:<br/>
	 * <code>SecurityService.getInstance().check(Roles.DATA_MANAGER, someProtocol, true);</code>
	 * </p>
	 * 
	 * @param requiredRole
	 *            The role the user must have on the object.
	 * @param object
	 *            The object against which the permission is checked.
	 * @param showDialog
	 *            True to display an error dialog if the check fails.
	 * @param throwException
	 * 			  True to throw a PermissionDeniedException if the check fails.
	 * @return True if the permission check passed, false otherwise.
	 */
	public boolean check(String userName, String requiredRole, Object object, boolean showDialog, boolean throwException) {
		boolean access = hasRole(userName, requiredRole, object);
		if (!access) {
			String highestRole = getHighestRole(userName, object);
			highestRole = (highestRole == null) ? "None" : highestRole;
			if (showDialog) {
				AccessDialog.openAccessDeniedDialog(requiredRole, highestRole);
			}
			if (throwException) {
				throw new PermissionDeniedException(
						"You do not have permission to perform"
								+ " this operation. Your role: " + highestRole
								+ ", required role: " + requiredRole);
			}
		}
		return access;
	}

	private boolean hasRole(String userName, String requiredRole, Object object) {
		if (isGlobalAdmin(userName)) return true;
		String highestRole = getHighestRole(userName, object);
		if (highestRole == null) return false;
		return Roles.extendsRole(highestRole, requiredRole);
	}

	private String getHighestRole(String userName, Object object) {
		String highestRole = null;
		Set<Group> groups = getMemberships(userName);
		
		if (object == null) {
			// Get the highest role that is not bound to any particular team.
			for (Group g: groups) {
				if (highestRole == null || Roles.extendsRole(g.getRole(), highestRole)) highestRole = g.getRole();
			}
			return highestRole;
		}

		IOwnedObject ownedObject = null;
		if (object instanceof IOwnedObject) {
			ownedObject = (IOwnedObject)object;
		} else if (object instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable)object;
			ownedObject = (IOwnedObject)adaptable.getAdapter(IOwnedObject.class);
		}
		if (ownedObject == null) return getHighestRole(userName, null);

		String[] teams = ownedObject.getOwners();
		if (teams.length == 0) {
			// Get the highest role that is not bound to any particular team.
			for (Group g: groups) {
				if (highestRole == null || Roles.extendsRole(g.getRole(), highestRole)) highestRole = g.getRole();
			}
			return highestRole;
		}
		
		for (Group group : groups) {
			for (String team: teams) {
				boolean hasAccess = group.getTeam().equalsIgnoreCase(team) || group.getTeam().equals(Group.GLOBAL_TEAM);
				if (hasAccess) {
					String role = group.getRole();
					if (highestRole == null || Roles.extendsRole(role, highestRole)) {
						highestRole = role;
					}
				}
			}
		}

		return highestRole;
	}

	/*
	 * **************
	 * Personal Permission API
	 * **************
	 */

	public boolean checkPersonalObjectWithException(Action action, Object object) {
		return checkPersonalObject(action, getCurrentUserName(), object, false, true);
	}

	public boolean checkPersonalObject(Action action, Object object) {
		return checkPersonalObject(action, getCurrentUserName(), object, false, false);
	}

	public boolean checkPersonalObject(Action action, String userName, Object object, boolean showDialog, boolean throwException) {
		IOwnedPersonalObject ownedObject = null;
		if (object instanceof IOwnedPersonalObject) {
			ownedObject = (IOwnedPersonalObject) object;
		} else if (object instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) object;
			ownedObject = (IOwnedPersonalObject) adaptable.getAdapter(IOwnedPersonalObject.class);
		}

		// Owner always has access. So does the admin.
		boolean hasAccess = false;
		if (ownedObject != null) {
			hasAccess = ownedObject.getOwner().equalsIgnoreCase(userName) || isGlobalAdmin(userName);

			if (!hasAccess) {
				// This check is only required if the accessing user is not the owner or an admin.
				switch (ownedObject.getAccessScope()) {
				case PRIVATE:
					// Owner was already given access. Do nothing.
					break;
				case TEAM:
					// Since the accessing user is not the owner, see if he's part of the team.
					hasAccess = getHighestRole(userName, object) != null;
				case PUBLIC_R:
					// We're reading here, everyone can read.
					hasAccess = Action.READ.hasRights(action);
					break;
				case PUBLIC_RU:
					// We're reading here, everyone can read/update.
					hasAccess = Action.UPDATE.hasRights(action);
					break;
				case PUBLIC_RUD:
					// We're reading here, everyone can read/update/delete.
					hasAccess = Action.DELETE.hasRights(action);
					break;
				}
			}
		}

		if (!hasAccess) {
			if (showDialog) {
				AccessDialog.openAccessDeniedDialog(null, getHighestRole(userName, ownedObject));
			}
			if (throwException) {
				throw new PermissionDeniedException("You do not have permission to perform this operation.");
			}
		}

		return hasAccess;
	}

	/*
	 * **************
	 * Membership API
	 * **************
	 */

	/**
	 * Helper method to get all groups the specified user is member of.
	 */
	public Set<Group> getMemberships(String userName) {
		Set<Group> groups = new HashSet<Group>();
		for (Group group : securityConfig.keySet()) {
			List<String> members = securityConfig.get(group);
			if (members.contains(userName) || members.contains("*")) {
				groups.add(group);
			}
		}
		return groups;
	}

	/**
	 * Helper method to get all groups the specified user is member of with
	 * the required role.
	 */
	public Set<Group> getMemberships(String userName, String requiredRole) {
		Set<Group> allMemberships = getMemberships(userName);
		Set<Group> matchingMemberships = new HashSet<Group>();
		for (Group group : allMemberships) {
			if (requiredRole == null || Roles.extendsRole(group.getRole(), requiredRole)) {
				matchingMemberships.add(group);
			}
		}
		return matchingMemberships;
	}

	/**
	 * Find all teams a user is member of via at least one
	 * group membership.
	 * 
	 * @param userName The user to check.
	 * @return A Set of team names the user is member of.
	 */
	public Set<String> getTeams(String userName) {
		Set<Group> groups = getMemberships(userName, null);
		Set<String> teams = new HashSet<String>();
		for (Group g: groups) {
			teams.add(g.getTeam());
		}
		return teams;
	}

	/**
	 * Get all users, including their group memberships.
	 */
	public Set<UserContext> getAllUsers() {
		Set<UserContext> userContexts = new HashSet<UserContext>();

		// Get all unique user names.
		Set<String> users = new HashSet<String>();
		for (Group group : securityConfig.keySet()) {
			users.addAll(securityConfig.get(group));
		}

		for (String username : users) {
			UserContext context = new UserContext(username, getMemberships(username));
			userContexts.add(context);
		}

		return userContexts;
	}

	/**
	 * Get all known teams.
	 */
	public Set<String> getAllTeams() {
		Set<String> teams = new HashSet<String>();
		for (Group group: securityConfig.keySet()) {
			teams.add(group.getTeam());
		}
		return teams;
	}

	/**
	 * Get all teams the current user has access to.
	 */
	public Set<String> getAccessibleTeams() {
		if (isGlobalAdmin()) return getAllTeams();
		return getTeams(getCurrentUserName());
	}
	
	public enum Action {
		NONE(-1)
		, CREATE(0)
		, READ(2)
		, UPDATE(4)
		, DELETE(8)
		;

		private int rights;

		private Action(int rights) {
			this.rights = rights;
		}

		public boolean hasRights(Action action) {
			return rights >= action.rights;
		}
	}
	
	private static class EmbeddedLoginHandler implements ILoginHandler {
		@Override
		public void authenticate(String userName, byte[] password) throws AuthenticationException {
			// Accept any authentication, treat user as global admin.
			SecurityService.getInstance().setCurrentUser(userName);
			Map<Group, List<String>> groups = new HashMap<Group, List<String>>();
			groups.put(Group.GLOBAL_ADMIN_GROUP, Collections.singletonList(SecurityService.getInstance().getCurrentUserName()));
			SecurityService.getInstance().setSecurityConfig(groups);
		}
	}
}
