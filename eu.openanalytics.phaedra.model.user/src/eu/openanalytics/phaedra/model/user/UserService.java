package eu.openanalytics.phaedra.model.user;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.naming.directory.DirContext;
import javax.persistence.PersistenceException;

import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.cache.CacheConfig;
import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.db.jpa.BaseJPAService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.AuthConfig;
import eu.openanalytics.phaedra.base.security.SecurityService;
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
	private ICache preferenceCache;
	
	private UserService() {
		// Hidden constructor
		CacheConfig cfg = new CacheConfig("PreferenceCache");
		cfg.maxBytes = 0;
		cfg.tti = 0;
		cfg.ttl = 0;
		preferenceCache = CacheService.getInstance().createCache(cfg);
	}

	public static UserService getInstance() {
		return instance;
	}

	/*
	 * **********
	 * Public API
	 * **********
	 */

	public List<User> getUsers() {
		return getList(User.class);
	}

	public User getUser(String userName) {
		String query = "select u from User u where u.userName = ?1";
		return getEntity(query, User.class, userName.toUpperCase());
	}

	public List<UserActivity> getAllUserActivity() {
		List<UserActivity> activities = new ArrayList<>();
		try (Connection conn = Screening.getEnvironment().getJDBCConnection()) {
			String queryString = "select s.user_code, s.login_date, s.host, s.version,"
					+ " (select count(*) from phaedra.hca_user_session where user_code = s.user_code) as login_count"
					+ " from phaedra.hca_user_session s where s.session_id in"
					+ " (select max(session_id) as latest_session from"
					+ " (select * from phaedra.hca_user_session order by login_date desc) as subquery group by user_code)"
					+ " order by s.login_date desc";
			ResultSet rs = conn.createStatement().executeQuery(queryString);
			while (rs.next()) {
				activities.add(new UserActivity(
						rs.getString(1),
						rs.getInt(5),
						rs.getTimestamp(2),
						rs.getString(3),
						rs.getString(4)));
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
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
		
		initializePreferences();
	}

	public String getMailAddress(String userName) {
		String email = "";
		DirContext ctx = null;
		try {
			// Attempt to bind as the current user and then look up the target user's email address in LDAP.
			// Note that this only works if the current user is a service account with its password stored.
			AuthConfig ldapConfig = SecurityService.getInstance().getLdapConfig();
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

	public List<Preference> getPreferences(String type) {
		List<Preference> prefs = new ArrayList<>();
		for (Object key: preferenceCache.getKeys()) {
			CacheKey cKey = (CacheKey) key;
			if (cKey.getKeyPart(0).equals(type)) prefs.add((Preference) preferenceCache.get(cKey));
		}
		return prefs;
	}

	public void savePreferences(List<Preference> prefs) {
		saveCollection(prefs);
	}

	public void deletePreference(Preference pref) {
		delete(pref);
	}

	public String getPreferenceValue(String type, String item) {
		Preference pref = getPreference(type, item);
		return (pref == null) ? null : pref.getValue();
	}

	public void setPreferenceValue(String type, String item, String value) {
		Preference pref = getPreference(type, item);
		if (pref == null) {
			pref = new Preference();
			pref.setUser(SecurityService.getInstance().getCurrentUserName());
			pref.setType(type);
			pref.setItem(item);
			
			CacheKey key = getPrefCacheKey(type, item);
			preferenceCache.put(key, pref);
		}
		pref.setValue(value);
		save(pref);
	}
	
	private void initializePreferences() {
		String userName = SecurityService.getInstance().getCurrentUserName();
		String query = "select pref from Preference pref where pref.user = ?1";
		List<Preference> prefs = getList(query, Preference.class, userName);
		for (Preference pref: prefs) {
			CacheKey key = getPrefCacheKey(pref.getType(), pref.getItem());
			preferenceCache.put(key, pref);
		}
	}
	
	private Preference getPreference(String type, String item) {
		CacheKey key = getPrefCacheKey(type, item);
		Preference pref = (Preference) preferenceCache.get(key);
		return pref;
	}
	
	private CacheKey getPrefCacheKey(String type, String item) {
		return CacheKey.create(type, item);
	}
}