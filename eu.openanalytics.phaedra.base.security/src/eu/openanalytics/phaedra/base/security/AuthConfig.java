package eu.openanalytics.phaedra.base.security;

import eu.openanalytics.phaedra.base.util.misc.ConfigResolver;

public class AuthConfig extends ConfigResolver {

	// LDAP config
	public static final String URL = "url";
	public static final String AUTH_TYPE = "type";
	public static final String DEFAULT_DOMAIN = "default.domain";
	public static final String GROUP_PREFIX = "group.prefix";
	public static final String GROUP_FILTER = "group.filter";
	public static final String PRINCIPAL_MAPPING = "principal.mapping";
	public static final String USERNAME_ATTRIBUTE = "username.attribute";
	
	// Windows config
	public static final String WIN_LOGON = "win.logon";
	public static final String GLOBAL_ROLE = "global.role";
	
	public AuthConfig() {
		super("phaedra.auth.");
	}
}
