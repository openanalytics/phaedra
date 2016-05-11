package eu.openanalytics.phaedra.app.headless;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.app.headless.authenticate.Authentication;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

@SuppressWarnings("restriction")
public class Application implements IApplication {
	
	private static final String ISTARTUP_ID = "eu.openanalytics.phaedra.app.headless.headlessStartup";
	
	private static AtomicBoolean run = new AtomicBoolean(true);
	
	@Override
	public Object start(IApplicationContext context) throws Exception {
		
		if (!Authentication.authenticate()) return IApplication.EXIT_OK;
		
		createShutdownHook();
		executeStartupExtensions();

		Display display = PlatformUI.createDisplay();
		while(run.get()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		return IApplication.EXIT_OK;
		
	}

	@Override
	public void stop() {
		onExit();
	}
	
	private void executeStartupExtensions() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ISTARTUP_ID);
		for (IConfigurationElement element : config) {
			try {
				final Object o = element.createExecutableExtension("class");
				if (o instanceof IStartup) ((IStartup)o).earlyStartup();
			} catch (CoreException e) {
				EclipseLog.warn("Autostart failed for " + element.getAttribute("class"), Activator.getDefault());
			}
		}
	}

	private void createShutdownHook() {
		Thread hook = new Thread(){
			@Override
			public void run() {
				try {
					// First, stop the running application loop.
					shutdown();
					// Then, shut down the Phaedra environment.
					onExit();
					// Finally, shut down Equinox platform.
					EclipseStarter.shutdown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		Runtime.getRuntime().addShutdownHook(hook);
	}
	
	private void onExit() {
		Screening.getEnvironment().disconnect();
	}
	
	public static void shutdown() {
		Application.run.set(false);
	}

}
