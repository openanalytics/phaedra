package eu.openanalytics.phaedra.app;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.search.SearchService;
import eu.openanalytics.phaedra.base.search.model.QueryException;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class OpenURLProcessor implements Listener {

	private List<String> urlsToOpen = new ArrayList<>();
	
	private final static String PHAEDRA_URL_PROTOCOL = "phaedra://";
	
	public OpenURLProcessor(Display display) {
		display.addListener(SWT.OpenDocument, this);
		
		// If the program started with a url argument, process it now.
		BundleContext context = Activator.getDefault().getBundle().getBundleContext();
		ServiceReference<?> infoRef = context.getServiceReference(EnvironmentInfo.class.getName());
	    if (infoRef != null) {
	    	EnvironmentInfo envInfo = (EnvironmentInfo) context.getService(infoRef);
	    	String[] args = envInfo.getCommandLineArgs();
	    	for (String arg: args) {
	    		if (arg.toLowerCase().startsWith(PHAEDRA_URL_PROTOCOL)) {
	    			Event event = new Event();
	    			event.text = arg;
	    			handleEvent(event);
	    		}
	    	}
	    }
	}
	
	@Override
	public void handleEvent(Event event) {
		String url = event.text;
		urlsToOpen.add(url);
		Activator.getDefault().getLog().log(new Status(IStatus.INFO, Activator.PLUGIN_ID, "URL received: " + url));
	}
	
	public void catchUp(Display display) {
		if (urlsToOpen.isEmpty()) return;
		
		// If we start supporting events that can arrive on a non-UI thread, the following
		// lines will need to be in a "synchronized" block:
		String[] urls = new String[urlsToOpen.size()];
		urlsToOpen.toArray(urls);
		urlsToOpen.clear();

		for (int i = 0; i < urls.length; i++) {
			openURL(display, urls[i]);
		}
	}
	
	private void openURL(Display display, String url) {
		/*
		 * Open objects by URL, e.g.:
		 * 
		 * phaedra://plate/45711
		 * phaedra://experiment/2090
		 */
		
		if (url == null || !url.toLowerCase().startsWith(PHAEDRA_URL_PROTOCOL)) return;
		url = url.substring(PHAEDRA_URL_PROTOCOL.length());
		String[] parts = url.split("/");
		if (parts.length < 2) return;
		
		String typeName = parts[0].toLowerCase();
		long id = Long.valueOf(parts[1]);
		try {
			PlatformObject obj = SearchService.getInstance().searchById(typeName, id);
			if (obj instanceof IValueObject) EditorFactory.getInstance().openEditor((IValueObject) obj);
		} catch (QueryException e) {
			EclipseLog.error("Failed to process URL: " + url, e, Activator.getDefault());
		}
	}
}
