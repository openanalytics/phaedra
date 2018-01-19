package eu.openanalytics.phaedra.base.security.ldap;

import eu.openanalytics.phaedra.base.util.misc.ConfigResolver;

public class LDAPConfig extends ConfigResolver {

	public static final String URL = "url";
	public static final String AUTH_TYPE = "type";
	public static final String DEFAULT_DOMAIN = "default.domain";
	public static final String GROUP_PREFIX = "group.prefix";
	public static final String GROUP_FILTER = "group.filter";
	public static final String PRINCIPAL_MAPPING = "principal.mapping";
	public static final String USERNAME_ATTRIBUTE = "username.attribute";
	
	public LDAPConfig() {
		super("phaedra.auth.");
	}
}
