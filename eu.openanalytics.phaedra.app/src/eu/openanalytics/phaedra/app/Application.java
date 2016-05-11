package eu.openanalytics.phaedra.app;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.pref.store.GlobalPrefenceAccessor;
import eu.openanalytics.phaedra.base.util.io.FileUtils;

public class Application implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		Display display = PlatformUI.createDisplay();
		try {
			OpenURLProcessor openURLProcessor = new OpenURLProcessor(display);
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor(openURLProcessor));
			if (returnCode == PlatformUI.RETURN_RESTART) return IApplication.EXIT_RESTART;
			else return IApplication.EXIT_OK;
		} finally {
			onExit();
			display.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	@Override
	public void stop() {
		onExit();
		if (!PlatformUI.isWorkbenchRunning()) return;
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final Display display = workbench.getDisplay();
		display.syncExec(() -> {
			if (!display.isDisposed()) workbench.close();
		});
	}

	private void onExit() {
		FileUtils.clearTempFolder();
		GlobalPrefenceAccessor.savePreferences();
		Screening.getEnvironment().disconnect();
		CacheService.getInstance().shutdown();
	}
}
