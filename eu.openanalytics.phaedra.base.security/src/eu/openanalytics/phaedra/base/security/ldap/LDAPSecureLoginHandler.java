package eu.openanalytics.phaedra.base.security.ldap;

import java.util.Arrays;
import java.util.Collection;

import javax.naming.directory.DirContext;

import eu.openanalytics.phaedra.base.security.AbstractLoginHandler;
import eu.openanalytics.phaedra.base.security.Activator;
import eu.openanalytics.phaedra.base.security.AuthConfig;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

/**
 * This login handler is geared towards Active Directory.
 * It assumes the existence of attributes such as NAME, MEMBER and SAMACCOUNTNAME.
 */
public class LDAPSecureLoginHandler extends AbstractLoginHandler {
	
	
	public LDAPSecureLoginHandler(final SecurityService securityService, final AuthConfig authConfig) {
		super(securityService, authConfig);
	}
	
	
	@Override
	public Collection<String> getRequiredParameter() {
		return Arrays.asList(USERNAME, PASSWORD);
	}
	
	@Override
	public void authenticate(String userName, byte[] password, boolean setUserContext) {
		DirContext ctx = null;
	    try {
	    	AuthConfig ldapConfig = getAuthConfig();
	    	ctx = LDAPUtils.bind(userName, password, ldapConfig);
	    	if (setUserContext) {
	    		final SecurityService securityService = getSecurityService();
	    		securityService.setCurrentUser(userName);
		    	securityService.setSecurityConfig(LDAPUtils.loadGroups(ctx, ldapConfig));
	    	}
		} finally {
			if (ctx != null) try { ctx.close(); } catch (Exception e) {}
		}
	    
	    if (setUserContext) {
		    try {
		    	final SecurityService securityService = getSecurityService();
				securityService.registerAPIToken(userName, new String(password));
			} catch (Exception e) {
				EclipseLog.error("Failed to obtain API token", e, Activator.PLUGIN_ID);
			}
	    }
	}

}
