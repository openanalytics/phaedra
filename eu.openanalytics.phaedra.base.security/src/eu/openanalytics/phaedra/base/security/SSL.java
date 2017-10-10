package eu.openanalytics.phaedra.base.security;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;

public class SSL {

	private static SSLContext defaultSSLContext;
	private static SSLContext platformSSLContext;

	static {
		try {
			defaultSSLContext = SSLContext.getDefault();
			platformSSLContext = defaultSSLContext;
			
			if (ProcessUtils.isWindows()) {
				TrustManagerFactory trustFac = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				KeyStore trustStore = KeyStore.getInstance("WINDOWS-ROOT");
				trustStore.load(null, null);
				trustFac.init(trustStore);

				platformSSLContext = SSLContext.getInstance("SSL");
				platformSSLContext.init(null, trustFac.getTrustManagers(), null);
			}
		} catch (Exception e) {
			EclipseLog.warn("Cannot initalize SSL context using Windows provider, using Default provider", e, Activator.getDefault());
			platformSSLContext = defaultSSLContext;
		}
	}

	public static void activatePlatformSSL() {
		SSLContext.setDefault(platformSSLContext);
	}
	
	public static void activateDefaultSSL() {
		SSLContext.setDefault(defaultSSLContext);
	}
}
