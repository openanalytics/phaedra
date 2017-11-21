package eu.openanalytics.phaedra.model.user;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.directory.DirContext;
import javax.persistence.EntityManager;

import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.ldap.LDAPConfig;
import eu.openanalytics.phaedra.base.security.ldap.LDAPUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.model.user.util.UserActivity;
import eu.openanalytics.phaedra.model.user.vo.Preference;
import eu.openanalytics.phaedra.model.user.vo.User;
import eu.openanalytics.phaedra.model.user.vo.UserSession;

/**
 * This service controls user-related information, including:
 * <ul>
 * <li>Retrieving user names, mail addresses and login history</li>
 * <li>Retrieving and storing user preferences</li>
 * </ul>
 */
public class UserService extends BaseJPAService {

	private static UserService instance = new UserService();

	private UserService() {
		// Hidden constructor
	}

	public static UserService getInstance() {
		return instance;
	}

	@Override
	protected EntityManager getEntityManager() {
		return Screening.getEnvironment().getEntityManager();
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	/* User & UserLog */

	public List<User> getUsers() {
		EntityManager em = Screening.getEnvironment().getEntityManager();
		List<User> users = getList(User.class);
		// By refreshing we are sure to have recent data, e.g. updated by other clients.
		for (User user: users) em.refresh(user);
		return users;
	}

	public User getUser(String userName) {
		String query = "select u from User u where u.userName = ?1";
		return getEntity(query, User.class, userName.toUpperCase());
	}

	public List<UserActivity> getAllUserActivity() {
		List<UserSession> sessions = getList(UserSession.class);

		Map<User, List<UserSession>> userSessions = new HashMap<>();
		for (UserSession session: sessions) {
			// Fixes the strange issue that JPA slows down enormously due to
			// many UserSession objects staying in an internal object change map.
			getEntityManager().detach(session);
			
			List<UserSession> userSessionList = userSessions.get(session.getUser());
			if (userSessionList == null) {
				userSessionList = new ArrayList<>();
				userSessions.put(session.getUser(), userSessionList);
			}
			userSessionList.add(session);
		}
		List<UserActivity> activities = new ArrayList<>();
		for (User user: userSessions.keySet()) {
			activities.add(new UserActivity(user, userSessions.get(user)));
		}
		return activities;
	}

	public void logSession(String userName, String host, String version) {
		try {
			if (host == null) {
				host = "unknown";
				try {
					InetAddress address = InetAddress.getLocalHost();
					host = address.getHostName();
				} catch (UnknownHostException e) {}
			}
			if (version == null) version = "unknown";

			userName = userName.toUpperCase();

			// Create or update the User object.
			User user = getEntity(User.class, userName);
			if (user == null) {
				user = new User();
				user.setUserName(userName);
			}
			user.setLastLogon(new Date());
			save(user);

			// Insert a user session.
			UserSession session = new UserSession();
			session.setHost(host);
			session.setLoginDate(new Date());
			session.setVersion(version);
			session.setUser(user);
			save(session);

		} catch (Exception e) {
			EclipseLog.warn("Failed to log user session", e, Activator.getDefault());
		}
	}

	public String getMailAddress(String userName) {
		String email = "";
		DirContext ctx = null;
		try {
			// Attempt to bind as the current user and then look up the target user's email address in LDAP.
			// Note that this only works if the current user is a service account with its password stored.
			LDAPConfig ldapConfig = SecurityService.getInstance().getLdapConfig();
			String currentUserName = SecurityService.getInstance().getCurrentUserName();
			String password = Screening.getEnvironment().getConfig().resolvePassword(currentUserName);
			ctx = LDAPUtils.bind(currentUserName, password.getBytes(), ldapConfig);
			email = LDAPUtils.lookupEmail(userName, ctx, ldapConfig);
			email = StringUtils.isValidEmail(email)? email.toLowerCase() : "";
		} catch (IOException e) {
			Activator.getDefault().getLog().log(
					new Status(Status.WARNING, Activator.PLUGIN_ID, "Failed to retrieve email addresses. " + e.getMessage()));
		} finally {
			if (ctx != null) try { ctx.close(); } catch (Exception e) {}
		}
		return email;
	}

	/* Preferences */

	/*
	 * Note: do not use these methods to access UserPreference type preferences directly.
	 * Those are handled by the global preference persistor and accessed locally by
	 * a plugins' Activator.getPreferenceStore().
	 */

	public List<Preference> getPreferences(String type) {
		String userName = SecurityService.getInstance().getCurrentUserName();
		String query = "select pref from Preference pref where pref.type = ?1 and pref.user = ?2";
		return getList(query, Preference.class, type, userName);
	}

	public void savePreferences(List<Preference> prefs) {
		saveCollection(prefs);
	}

	public void deletePreference(Preference pref) {
		delete(pref);
	}

	public String getPreferenceValue(String type, String item) {
		String userName = SecurityService.getInstance().getCurrentUserName();
		String query = "select pref from Preference pref where pref.type = ?1 and pref.user = ?2 and pref.item = ?3";
		Preference pref = getEntity(query, Preference.class, type, userName, item);
		if (pref != null) return pref.getValue();
		return null;
	}

	public void setPreferenceValue(String type, String item, String value) {
		String userName = SecurityService.getInstance().getCurrentUserName();
		String query = "select pref from Preference pref where pref.type = ?1 and pref.user = ?2 and pref.item = ?3";
		Preference pref = getEntity(query, Preference.class, type, userName, item);
		if (pref == null) {
			pref = new Preference();
			pref.setUser(userName);
			pref.setType(type);
			pref.setItem(item);
		}
		pref.setValue(value);
		save(pref);
	}
}