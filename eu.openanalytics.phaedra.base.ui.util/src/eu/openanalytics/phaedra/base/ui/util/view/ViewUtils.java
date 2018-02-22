package eu.openanalytics.phaedra.base.ui.util.view;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class ViewUtils {

	private static final String IVIEWCONSTRUCTOR_ID = "org.eclipse.ui.views";

	public static IViewPart constructView(String className, Composite composite) throws CoreException {
		return constructView(className, composite, null);
	}

	public static IViewPart constructView(String className, Composite composite, ToolBar toolBar) throws CoreException {

		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IVIEWCONSTRUCTOR_ID);

		for (IConfigurationElement element : config) {
			if (!className.equals(element.getAttribute("class"))) continue;
			final Object o = element.createExecutableExtension("class");
			if (o instanceof IViewPart) {
				IViewPart view = (IViewPart) o;

				// Create a dummy viewsite.
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IViewPart tempView = page.showView(element.getAttribute("id"));
				try {
					ReflectionUtils.setFieldObject(tempView.getViewSite().getActionBars(), "toolbarManager", new ToolBarManager(toolBar));
				} catch (IllegalAccessException e) {}
				view.init(tempView.getViewSite(), null);
				view.createPartControl(composite);
				page.setPartState(page.getReference(tempView), IWorkbenchPage.STATE_MINIMIZED);
				return view;
			}
		}
		return null;
	}

}
