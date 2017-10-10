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

import eu.openanalytics.phaedra.base.security.AuthenticationException;
import eu.openanalytics.phaedra.base.security.SSL;
import eu.openanalytics.phaedra.base.security.model.Group;
import eu.openanalytics.phaedra.base.security.model.Roles;

public class LDAPUtils {

	private static final String PERSON_FILTER = "(objectCategory=person)";
	private static final String DEFAULT_USERNAME_ATTR = "sAMAccountName";
	private static final String NAME_ATTR = "name";
	private static final String MEMBER_ATTR = "member";
	private static final String MAIL_ATTR = "mail";
	
	public static DirContext bind(String userName, byte[] password, LDAPConfig cfg) {
		// LDAP bind may need a qualified userName, including domain.
		if (cfg.defaultDomain != null && !cfg.defaultDomain.isEmpty() && !userName.contains("\\")) {
			userName = cfg.defaultDomain + "\\" + userName;
		}
		
		String principal = userName;
		if (cfg.principalMapping != null && !cfg.principalMapping.isEmpty()) {
			principal = cfg.principalMapping.replace("${username}", userName);
		}
		
		if (password.length == 0) {
			throw new AuthenticationException("Password cannot be empty");
		}
		
		try {
			SSL.activatePlatformSSL();
			
			Hashtable<String, String> env = new Hashtable<>(11);
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, cfg.ldapUrl);
			env.put(Context.SECURITY_PRINCIPAL, principal);
			env.put(Context.SECURITY_CREDENTIALS, new String(password));
			
			DirContext ctx = new InitialDirContext(env);
			return ctx;
			
		} catch (Throwable t) {
			throw new AuthenticationException("Authentication failed. Please check the username and password.", t);
		} finally {
			SSL.activateDefaultSSL();
		}
	}
	
	public static String lookupEmail(String username, DirContext ctx, LDAPConfig cfg) {
		try {
			String usernameAttribute = cfg.usernameAttribute;
			if (usernameAttribute == null) usernameAttribute = DEFAULT_USERNAME_ATTR;
			
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
	
	protected static Map<Group, List<String>> loadGroups(DirContext ctx, LDAPConfig cfg) {
		Map<Group, List<String>> groups = new HashMap<Group, List<String>>();
		
		if (cfg.groupPrefix == null || cfg.groupPrefix.isEmpty()) {
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
			NamingEnumeration<SearchResult> resultSet = ctx.search(cfg.groupFilter, "(" + NAME_ATTR + "=" + cfg.groupPrefix + "*)", ctrl);
			
			while (resultSet.hasMore()) {
				// Each entry is one Afrax group.
				SearchResult entry = resultSet.next();

				// Extract team and role name from the Afrax group name.
				String team = null;
				String role = null;
				String groupName = entry.getAttributes().get(NAME_ATTR).get().toString();
				groupName = groupName.substring(cfg.groupPrefix.length());
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

				String usernameAttribute = cfg.usernameAttribute;
				if (usernameAttribute == null) usernameAttribute = DEFAULT_USERNAME_ATTR;
				
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