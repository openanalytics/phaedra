package eu.openanalytics.phaedra.base.internal.security.oidc;

import java.io.IOException;
import java.net.URI;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;


public class OidcHttpService {
	
	
	public static final String WEBAPP_NAME= "oidc"; //$NON-NLS-1$
	public static final String CONTEXT_PATH = "/oidc"; //$NON-NLS-1$
	
	public static final String AUTH_RESPONSE_PATH = "/auth/response"; //$NON-NLS-1$
	
	
	public static interface HttpResponseHandler {
		
		
		boolean handle(HTTPRequest response);
		
		void cancel();
		
	}
	
	
	private static OidcHttpService instance;
	
	static synchronized OidcHttpService getHttpService() throws IOException {
		OidcHttpService service = instance;
		if (service == null) {
			try {
				service = new OidcHttpService();
				instance = service;
			}
			catch (final Exception e) {
				throw new IOException("Failed to initialize service for OIDC.", e);
			}
		}
		return service;
	}
	
	
	private final JettyServer httpServer;
	
	private final Object authExchangeLock = new Object();
	private HttpResponseHandler authExchangeHandler;
	
	
	private OidcHttpService() throws Exception {
		this.httpServer = new JettyServer();
		this.httpServer.startServer();
	}
	
	
	public String getRootUrl() {
		return "http://" + this.httpServer.getHost() + ":" + this.httpServer.getPort() + CONTEXT_PATH;
	}
	
	
	public URI startAuthExchange(final HttpResponseHandler handler) {
		final HttpResponseHandler prevHandler;
		synchronized (this.authExchangeLock) {
			prevHandler = this.authExchangeHandler;
			this.authExchangeHandler = handler;
		}
		if (prevHandler != null) {
			prevHandler.cancel();
		}
		return URI.create(getRootUrl() + AUTH_RESPONSE_PATH);
	}
	
	public boolean handleAuthReponse(final HTTPRequest response) {
		final HttpResponseHandler handler;
		synchronized (this.authExchangeLock) {
			handler = this.authExchangeHandler;
		}
		if (handler != null) {
			return handler.handle(response);
		}
		return false;
	}
	
	public void endAuthReponse(final HttpResponseHandler handler) {
		synchronized (this.authExchangeLock) {
			if (this.authExchangeHandler == handler) {
				this.authExchangeHandler = null;
			}
		}
	}
	
}
