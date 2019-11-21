package eu.openanalytics.phaedra.base.internal.security.oidc;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;


public class JettyServer {
	
	
	private static final String OTHER_INFO= "eu.openanalytics.phaedra.base.security.oidc.nimbus"; //$NON-NLS-1$
	
	
	private final String host;
	private int port;
	
	
	public JettyServer() {
		this.host= "localhost"; //$NON-NLS-1$
		this.port= -1;
	}
	
	
	public void startServer() throws Exception {
		final Bundle bundle= Platform.getBundle("org.eclipse.equinox.http.registry"); //$NON-NLS-1$
		if (bundle == null) {
			throw new IllegalStateException("bundle 'org.eclipse.equinox.http.registry' is missing."); //$NON-NLS-1$
		}
		
		final Dictionary<String, Object> dict= new Hashtable<>();
		dict.put(JettyConstants.HTTP_HOST, this.host);
		dict.put(JettyConstants.HTTP_PORT, Integer.valueOf((this.port == -1) ? 0 : this.port)); 
		
		dict.put(JettyConstants.CONTEXT_PATH, OidcHttpService.CONTEXT_PATH);
		dict.put(JettyConstants.OTHER_INFO, OTHER_INFO);
		
		dict.put(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL, Integer.valueOf(30 * 60)); // 30 minutes
		
		// suppress Jetty INFO/DEBUG messages to stderr
		Logger.getLogger("org.mortbay").setLevel(Level.WARNING); //$NON-NLS-1$	
		
		JettyConfigurator.startServer(OidcHttpService.WEBAPP_NAME, dict);
		
		if (bundle.getState() == Bundle.RESOLVED) {
			bundle.start(Bundle.START_TRANSIENT);
		}
		if (this.port == -1) {
			// Jetty selected a port number for us
			final ServiceReference[] reference= bundle.getBundleContext().getServiceReferences(
					"org.osgi.service.http.HttpService", "(" + JettyConstants.OTHER_INFO + "=" + OTHER_INFO + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			final Object assignedPort= reference[0].getProperty(JettyConstants.HTTP_PORT);
			this.port= Integer.parseInt((String)assignedPort);
		}
	}
	
	public void stopServer() throws Exception {
		JettyConfigurator.stopServer(OidcHttpService.WEBAPP_NAME);
		this.port= -1;
	}
	
	public String getHost() {
		return this.host;
	}
	
	public int getPort() {
		return this.port;
	}
	
}
