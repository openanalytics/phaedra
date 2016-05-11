package eu.openanalytics.phaedra.base.security.ldap;

import javax.naming.directory.DirContext;

import eu.openanalytics.phaedra.base.security.ILoginHandler;
import eu.openanalytics.phaedra.base.security.SecurityService;

/**
 * This login handler is geared towards Active Directory.
 * It assumes the existence of attributes such as NAME, MEMBER and SAMACCOUNTNAME.
 */
public class LDAPSecureLoginHandler implements ILoginHandler {

	public void authenticate(String userName, byte[] password) {
		DirContext ctx = null;
	    try {
	    	LDAPConfig ldapConfig = SecurityService.getInstance().getLdapConfig();
	    	ctx = LDAPUtils.bind(userName, password, ldapConfig);
	    	SecurityService.getInstance().setCurrentUser(userName);
	    	SecurityService.getInstance().setSecurityConfig(LDAPUtils.loadGroups(ctx, ldapConfig));
		} finally {
			if (ctx != null) try { ctx.close(); } catch (Exception e) {}
		}
	}

}