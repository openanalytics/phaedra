package eu.openanalytics.phaedra.base.security.ldap;

public class LDAPConfig {

	/**
	 * The URL pointing to the LDAP server.
	 */
	public String ldapUrl;
	
	/**
	 * The prefix that all Phaedra group names share.
	 */
	public String groupPrefix;
	
	/**
	 * The LDAP search filter for Phaedra groups.
	 */
	public String groupFilter;
	
	/**
	 * The LDAP attribute that specifies the username.
	 */
	public String usernameAttribute;
	
	/**
	 * The default domain for accounts who log in without a domain specified.
	 */
	public String defaultDomain;
	
	/**
	 * The mapping from the user name to the principal string.
	 */
	public String principalMapping;
}
