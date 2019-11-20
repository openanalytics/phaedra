package eu.openanalytics.phaedra.base.security.ldap;

import javax.naming.directory.DirContext;

import eu.openanalytics.phaedra.base.security.Activator;
import eu.openanalytics.phaedra.base.security.AuthConfig;
import eu.openanalytics.phaedra.base.security.ILoginHandler;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

/**
 * This login handler is geared towards Active Directory.
 * It assumes the existence of attributes such as NAME, MEMBER and SAMACCOUNTNAME.
 */
public class LDAPSecureLoginHandler implements ILoginHandler {

	public void authenticate(String userName, byte[] password, boolean setUserContext) {
		DirContext ctx = null;
	    try {
	    	AuthConfig ldapConfig = SecurityService.getInstance().getLdapConfig();
	    	ctx = LDAPUtils.bind(userName, password, ldapConfig);
	    	if (setUserContext) {
		    	SecurityService.getInstance().setCurrentUser(userName);
		    	SecurityService.getInstance().setSecurityConfig(LDAPUtils.loadGroups(ctx, ldapConfig));
	    	}
		} finally {
			if (ctx != null) try { ctx.close(); } catch (Exception e) {}
		}
	    
	    if (setUserContext) {
		    try {
			    SecurityService.getInstance().registerAPIToken(userName, new String(password));
			} catch (Exception e) {
				EclipseLog.error("Failed to obtain API token", e, Activator.PLUGIN_ID);
			}
	    }
	}

}