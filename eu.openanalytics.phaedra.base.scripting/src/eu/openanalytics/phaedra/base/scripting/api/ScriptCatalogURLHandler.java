package eu.openanalytics.phaedra.base.scripting.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;

public class ScriptCatalogURLHandler extends AbstractURLStreamHandlerService {

	private final static String PROTOCOL = "script";
	
	@Override
	public URLConnection openConnection(URL url) throws IOException {
		return new URLConnection(url) {
			
			@Override
			public void connect() throws IOException {
				// Do nothing.
			}
			
			@Override
			public InputStream getInputStream() throws IOException {
				String scriptName = url.toString().substring((PROTOCOL + "://").length());
				String scriptBody = ScriptService.getInstance().getCatalog().getScriptBody(scriptName);
				return new ByteArrayInputStream(scriptBody.getBytes());
			}
		};
	}

	public Dictionary<String, Object> getProperties() {
		Hashtable<String, Object> props = new Hashtable<>();
		props.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { PROTOCOL });
		return props;
	}
}
