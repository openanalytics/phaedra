package eu.openanalytics.phaedra.base.http.server.jetty.ssl;

import java.util.Dictionary;

import org.eclipse.equinox.http.jetty.JettyCustomizer;

public class JettyHTTPSCustomizer extends JettyCustomizer {

	@SuppressWarnings("rawtypes")
	@Override
	public Object customizeHttpsConnector(Object connector, Dictionary settings) {
		return new NewSSLSocketConnector();
	}
	
}
