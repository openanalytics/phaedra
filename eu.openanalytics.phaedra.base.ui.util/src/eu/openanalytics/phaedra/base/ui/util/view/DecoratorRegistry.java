package eu.openanalytics.phaedra.base.ui.util.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.ui.util.Activator;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;

public class DecoratorRegistry {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".decoratorProvider";
	public final static String ATTR_TARGET_CLASS = "targetClass";
	public final static String ATTR_DECORATOR = "decorator";

	public static List<PartDecorator> getDecoratorsFor(IDecoratedPart view) {
		List<PartDecorator> decorators = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				String targetClass = el.getAttribute(ATTR_TARGET_CLASS);
				boolean match = false;
				if (targetClass.equals(view.getClass().getName())) match = true;
				Class<?> viewClass = view.getClass();
				while (!match && viewClass != Object.class) {
					viewClass = viewClass.getSuperclass();
					if (targetClass.equals(viewClass.getName())) match = true;
				}
				if (match) {
					PartDecorator decorator = (PartDecorator)el.createExecutableExtension(ATTR_DECORATOR);
					decorators.add(decorator);
				}
			} catch (CoreException e) {
				EclipseLog.warn(e.getMessage(), e, Activator.getDefault());
				// Ignore invalid extensions.
			}
		}
		return decorators;
	}

}
