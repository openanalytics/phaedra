package eu.openanalytics.phaedra.base.security.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import eu.openanalytics.phaedra.base.security.AuthConfig;
import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.SSL;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.security.model.Roles;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;

public class LDAPUtils {

	private static final String PERSON_FILTER = "(objectCategory=person)";
	private static final String DEFAULT_USERNAME_ATTR = "sAMAccountName";
	private static final String NAME_ATTR = "name";
	private static final String MEMBER_ATTR = "member";
	private static final String MAIL_ATTR = "mail";
	
	private static final String PROP_AUTO_USERNAME = "phaedra.login.auto.username";
	private static final String PROP_AUTO_DOMAIN = "phaedra.login.auto.domain";
	
	public static String getAutoLoginName() {
		boolean autoUsername = Boolean.valueOf(System.getProperty(PROP_AUTO_USERNAME));
		boolean autoDomain= Boolean.valueOf(System.getProperty(PROP_AUTO_DOMAIN));
		
		String username = autoUsername ? System.getProperty("user.name") : null;
		String domain = autoDomain ? System.getenv("USERDOMAIN") : null;

		String login = "";
		if (username != null && !username.isEmpty()) {
			login = username;
			if (domain != null && !domain.isEmpty()) login = domain + "\\" + login;
		}
		return login;
	}
	
	public static DirContext bind(String userName, byte[] password, AuthConfig cfg) {

		if (password.length == 0) throw new AuthenticationException("Password cannot be empty");
		
		// If a default domain has been configured, prepend it to the username (AD only).
		String defaultDomain = cfg.get(AuthConfig.DEFAULT_DOMAIN);
		if (defaultDomain != null && !defaultDomain.isEmpty() && !userName.contains("\\")) {
			userName = defaultDomain + "\\" + userName;
		}
		
		// If a principal mapping has been configured, apply it (e.g. map username to DN).
		String principal = userName;
		String principalMapping = cfg.get(AuthConfig.PRINCIPAL_MAPPING);
		if (principalMapping != null && !principalMapping.isEmpty()) {
			principal = principalMapping.replace("${username}", userName);
		}
		
		String[] urls = new String[] { cfg.get(AuthConfig.URL) };
		if (urls[0].contains(",")) {
			urls = urls[0].split(",");
		}
		
		try {
			SSL.activatePlatformSSL();
			Throwable lastException = null;
			for (String url: urls) {
				try {
					Hashtable<String, String> env = new Hashtable<>(11);
					env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
					env.put(Context.PROVIDER_URL, url);
					env.put(Context.SECURITY_PRINCIPAL, principal);
					env.put(Context.SECURITY_CREDENTIALS, new String(password));
					env.put("com.sun.jndi.ldap.read.timeout", "10000");
					
					String authType = cfg.get(AuthConfig.AUTH_TYPE);
					if (authType != null && !authType.isEmpty()) env.put(Context.SECURITY_AUTHENTICATION, authType);
					
					DirContext ctx = new InitialDirContext(env);
					return ctx;
					
				} catch (Throwable t) {
					lastException = t;
				}
			}
			throw new AuthenticationException("Authentication failed: " + StringUtils.getStackTrace(lastException, 200));
		} finally {
			SSL.activateDefaultSSL();
		}
	}
	
	public static String lookupEmail(String username, DirContext ctx, AuthConfig cfg) {
		try {
			String usernameAttribute = cfg.get(AuthConfig.USERNAME_ATTRIBUTE);
			if (usernameAttribute == null || usernameAttribute.isEmpty()) usernameAttribute = DEFAULT_USERNAME_ATTR;
			
			SearchControls ctrl = new SearchControls();
			ctrl.setReturningAttributes(new String[] { MAIL_ATTR });
			ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<SearchResult> resultSet = ctx.search("", "(&" + PERSON_FILTER + "(" + usernameAttribute + "=" + username + "))", ctrl);

			while (resultSet.hasMore()) {
				SearchResult entry = resultSet.next();
				Attribute mailAttribute = entry.getAttributes().get(MAIL_ATTR);
				if (mailAttribute != null) return mailAttribute.get().toString();
			}
		} catch (Exception e) {
			throw new AuthenticationException("Failed to query LDAP user e-mail: " + e.getMessage(), e);
		}

		return null;
	}
	
	protected static Map<Group, List<String>> loadGroups(DirContext ctx, AuthConfig cfg) {
		Map<Group, List<String>> groups = new HashMap<Group, List<String>>();
		
		String groupPrefix = cfg.get(AuthConfig.GROUP_PREFIX);
		String groupFilter = cfg.get(AuthConfig.GROUP_FILTER);
		String usernameAttribute = cfg.get(AuthConfig.USERNAME_ATTRIBUTE);
		if (usernameAttribute == null || usernameAttribute.isEmpty()) usernameAttribute = DEFAULT_USERNAME_ATTR;
		
		// If no group config is provided, treat all users as admins.
		if (groupPrefix == null || groupPrefix.isEmpty()) {
			Group group = new Group(Group.GLOBAL_TEAM, Roles.ADMINISTRATOR);
			List<String> members = new ArrayList<>();
			members.add("*");
			groups.put(group, members);
			return groups;
		}
		
		try {
			SearchControls ctrl = new SearchControls();
			ctrl.setReturningAttributes(new String[] { NAME_ATTR, MEMBER_ATTR });
			ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration<SearchResult> resultSet = ctx.search(groupFilter, "(" + NAME_ATTR + "=" + groupPrefix + "*)", ctrl);
			
			while (resultSet.hasMore()) {
				// Each entry is one Afrax group.
				SearchResult entry = resultSet.next();

				// Extract team and role name from the Afrax group name.
				String team = null;
				String role = null;
				String groupName = entry.getAttributes().get(NAME_ATTR).get().toString();
				groupName = groupName.substring(groupPrefix.length());
				String[] parts = groupName.split("_");

				if (parts.length < 2) continue;
				if (parts[0].equalsIgnoreCase("NAS")) continue;
				team = parts[0];
				role = parts[1];

				Group group = new Group(team, role);
				List<String> members = groups.get(group);
				if (members == null) {
					members = new ArrayList<String>();
					groups.put(group, members);
				}

				Attribute memberLists = entry.getAttributes().get(MEMBER_ATTR);
				if (memberLists != null) {
					NamingEnumeration<?> values = memberLists.getAll();
					while (values.hasMore()) {
						String memberDN = values.next().toString();
						
						ctrl.setSearchScope(SearchControls.OBJECT_SCOPE);
						ctrl.setReturningAttributes(new String[] { usernameAttribute });
						NamingEnumeration<SearchResult> memberAccountName = ctx.search(memberDN, PERSON_FILTER, ctrl);
						
						while (memberAccountName.hasMore()) {
							SearchResult res = memberAccountName.next();
							Attribute accountName = res.getAttributes().get(usernameAttribute);
							if (accountName != null) {
								String name = accountName.get().toString();
								members.add(name.toLowerCase());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new AuthenticationException("Failed to query LDAP groups: " + e.getMessage(), e);
		}

		return groups;
	}
	
}