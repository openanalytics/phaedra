package eu.openanalytics.phaedra.base.http.server.jetty;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;

import eu.openanalytics.phaedra.base.http.server.IHttpService;


public class JettyHttpService implements IHttpService {
	
	private static List<String> ids = new ArrayList<>();
	
	private static final String PROP_HTTP_PORT = "jetty.http.port";
	private static final String PROP_HTTPS_PORT = "jetty.https.port";
	private static final String PROP_KEYSTORE = "javax.net.ssl.keyStore";
	private static final String PROP_KEYSTORE_PW = "javax.net.ssl.keyStorePassword";
	private static final String HTTP_HEADER_BUFFER_SIZE = "http.headerbuffersize";
	
	@Override
	public void startup() {
		String httpPort = getProperty(PROP_HTTP_PORT, "80");
		String httpsPort = getProperty(PROP_HTTPS_PORT, "443");
		ids.add(startService(Integer.parseInt(httpPort), Integer.parseInt(httpsPort)));
	}
	
	@Override
	public void shutdown() {
		try {
			for (String id: ids) JettyConfigurator.stopServer(id);
		} catch (Exception e) {}
	}

	private String startService(int port, int sslPort) {
		String id = "JettyWebServer" + port;
		try {
			Dictionary<String, Object> dictionary = new Hashtable<>();
			dictionary.put(HTTP_HEADER_BUFFER_SIZE, 16000);
			dictionary.put(JettyConstants.HTTP_PORT, port);
			String sslKeystore = getProperty(PROP_KEYSTORE, null);
			if (sslKeystore != null) {
				dictionary.put(JettyConstants.HTTPS_ENABLED, true);
				dictionary.put(JettyConstants.HTTPS_PORT, sslPort);
				dictionary.put(JettyConstants.SSL_KEYSTORE, sslKeystore);
				dictionary.put(JettyConstants.SSL_KEYPASSWORD, getProperty(PROP_KEYSTORE_PW, null));
				dictionary.put(JettyConstants.CUSTOMIZER_CLASS, "eu.openanalytics.phaedra.base.http.server.jetty.ssl.JettyHTTPSCustomizer");
			}
			JettyConfigurator.startServer(id, dictionary);
			Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "Jetty web server started at port " + port));
		} catch (Exception e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to start Jetty web server at port " + port, e));
		}
		return id;
	}
	
	private String getProperty(String name, String defaultValue) {
		String value = System.getProperty(name);
		if (value == null || value.isEmpty()) value = defaultValue;
		return value;
	}
}
