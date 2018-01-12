package eu.openanalytics.phaedra.base.security.ldap;

import java.util.function.Function;

public class LDAPConfig {

	public static final String URL = "url";
	public static final String AUTH_TYPE = "type";
	public static final String DEFAULT_DOMAIN = "default.domain";
	public static final String GROUP_PREFIX = "group.prefix";
	public static final String GROUP_FILTER = "group.filter";
	public static final String PRINCIPAL_MAPPING = "principal.mapping";
	public static final String USERNAME_ATTRIBUTE = "username.attribute";
	
	private Function<String, String> resolver;
	
	public LDAPConfig(Function<String, String> resolver) {
		this.resolver = resolver;
	}
	
	public String get(String name) {
		String value = resolver.apply(name);
		if (value == null || value.isEmpty()) value = System.getProperty("phaedra.auth." + name);
		return value;
	}
}
