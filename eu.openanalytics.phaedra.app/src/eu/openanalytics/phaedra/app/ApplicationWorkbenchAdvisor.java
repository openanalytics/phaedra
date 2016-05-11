package eu.openanalytics.phaedra.app;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.ide.IDE;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private OpenURLProcessor urlProcessor;
	
	public ApplicationWorkbenchAdvisor(OpenURLProcessor processor) {
		this.urlProcessor = processor;
	}
	
    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

	public String getInitialWindowPerspectiveId() {
		return DefaultPerspective.class.getName();
	}
	
	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		
		// Disable hardware acceleration on Swing/AWT, because it breaks TopCat on Win7.
		System.setProperty("sun.java2d.opengl", "false");
		System.setProperty("sun.java2d.d3d", "false");

		// Enforce automatic update check.
		IEclipsePreferences node = DefaultScope.INSTANCE.getNode("org.eclipse.equinox.p2.ui.sdk.scheduler");
		node.putBoolean("enabled", true);
		
		configurer.setSaveAndRestore(true);
		IDE.registerAdapters();
	}
	
	@Override
	public void postStartup() {
		ScriptedScheduler.registerJobs();
	}
	
	@Override
	public boolean preShutdown() {
		try {
			ResourcesPlugin.getWorkspace().save(true, null);
		} catch (CoreException e) {}
		
		return super.preShutdown();
	}
	
	@Override
	public void eventLoopIdle(Display display) {
		if (urlProcessor != null)
			urlProcessor.catchUp(display);
		super.eventLoopIdle(display);
	}
	
	//TODO Enable custom error support
//	@Override
//	public synchronized AbstractStatusHandler getWorkbenchErrorHandler() {
//		return new CustomStatusHandler();
//	}
//
//	private static class CustomStatusHandler extends WorkbenchErrorHandler {
//		public CustomStatusHandler() {
//			ErrorSupportProvider provider = new CustomErrorSupportProvider();
//			Policy.setErrorSupportProvider(provider);
//		}
//	}
}
