package eu.openanalytics.phaedra.base.security;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class SSL {
	
	private static SSLContext defaultSSLContext;
	private static SSLContext windowsSSLContext;
	
	static {
		try {
			defaultSSLContext = SSLContext.getDefault();
			
			// Note: personal certs are not needed here, only the trust store.
//			KeyManagerFactory keyFac = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
//			KeyStore keyStore = KeyStore.getInstance("WINDOWS-MY");
//			keyStore.load(null, null);
//			keyFac.init(keyStore, null);
			TrustManagerFactory trustFac = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			KeyStore trustStore = KeyStore.getInstance("WINDOWS-ROOT");
			trustStore.load(null, null);
			trustFac.init(trustStore);
			
			windowsSSLContext = SSLContext.getInstance("SSL");
//			windowsSSLContext.init(keyFac.getKeyManagers(), trustFac.getTrustManagers(), null);
			windowsSSLContext.init(null, trustFac.getTrustManagers(), null);
		} catch (Exception e) {
			EclipseLog.warn("Cannot initalize SSL context using Windows provider, using Default provider", e, Activator.getDefault());
			try { windowsSSLContext = SSLContext.getDefault(); } catch (NoSuchAlgorithmException e2) {
				EclipseLog.warn("Cannot initalize SSL context using Default provider, no SSL context available", e, Activator.getDefault());
			}
		}
	}
	
	public static SSLContext getDefaultSSLContext() {
		return defaultSSLContext;
	}
	
	public static SSLContext getWindowsSSLContext() {
		return windowsSSLContext;
	}
}
