package eu.openanalytics.phaedra.base.security;


public abstract class AbstractLoginHandler implements ILoginHandler {
	
	
	private final SecurityService securityService;
	private final AuthConfig authConfig;
	
	
	public AbstractLoginHandler(final SecurityService securityService, final AuthConfig authConfig) {
		this.securityService = securityService;
		this.authConfig = authConfig;
	}
	
	
	protected SecurityService getSecurityService() {
		return this.securityService;
	}
	
	public AuthConfig getAuthConfig() {
		return authConfig;
	}
	
	
}
