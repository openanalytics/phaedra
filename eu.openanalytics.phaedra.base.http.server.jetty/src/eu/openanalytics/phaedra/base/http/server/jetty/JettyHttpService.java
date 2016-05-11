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
	private static final String HTTP_HEADER_BUFFER_SIZE = "http.headerbuffersize";
	
	@Override
	public void startup() {
		String httpPort = System.getProperty(PROP_HTTP_PORT);
		if (httpPort == null || httpPort.isEmpty()) httpPort = "80";
		String httpsPort = System.getProperty(PROP_HTTPS_PORT);
		if (httpsPort == null || httpsPort.isEmpty()) httpsPort = "443";
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
			dictionary.put(JettyConstants.HTTPS_ENABLED, true);
			dictionary.put(JettyConstants.HTTPS_PORT, sslPort);
			dictionary.put(JettyConstants.CUSTOMIZER_CLASS, "eu.openanalytics.phaedra.base.http.server.jetty.ssl.JettyHTTPSCustomizer");
			JettyConfigurator.startServer(id, dictionary);
			Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "Jetty web server started at port " + port));
		} catch (Exception e) {
			Activator.getDefault().getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to start Jetty web server at port " + port, e));
		}
		return id;
	}
}
