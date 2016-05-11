package eu.openanalytics.phaedra.base.ui.icons;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;

public class IconRegistry {
	private final static IconRegistry INSTANCE = new IconRegistry();
	
	private Map<Class<?>, ImageDescriptor> defaultImageDescriptors = new HashMap<>();
	private Map<Class<?>, ImageDescriptor> createImageDescriptors = new HashMap<>();
	private Map<Class<?>, ImageDescriptor> deleteImageDescriptors = new HashMap<>();
	private Map<Class<?>, ImageDescriptor> updateImageDescriptors = new HashMap<>();
	
	private IconRegistry() {
		init();
	}
	
	public static IconRegistry getInstance() {
		return INSTANCE;
	}
	
	private void init() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IIconProvider.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IIconProvider.ATTR_CLASS);
				if (o instanceof IIconProvider) {
					IIconProvider<?> iconProvider = (IIconProvider<?>) o;
					putInMapIfAvailable(defaultImageDescriptors, iconProvider.getType(), iconProvider.getDefaultImageDescriptor());
					putInMapIfAvailable(createImageDescriptors, iconProvider.getType(), iconProvider.getCreateImageDescriptor());
					putInMapIfAvailable(deleteImageDescriptors, iconProvider.getType(), iconProvider.getDeleteImageDescriptor());
					putInMapIfAvailable(updateImageDescriptors, iconProvider.getType(), iconProvider.getUpdateImageDescriptor());
				}
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}
	
	private void putInMapIfAvailable(Map<Class<?>, ImageDescriptor> imageDescriptors, Class<?> type, ImageDescriptor imageDescriptor) {
		if (imageDescriptor != null) {
			imageDescriptors.put(type, imageDescriptor);
		}
	}
	
	public ImageDescriptor getDefaultImageDescriptorFor(Class<?> type) {
		return defaultImageDescriptors.get(type);
	}	

	public ImageDescriptor getCreateImageDescriptorFor(Class<?> type) {
		return createImageDescriptors.get(type);
	}	

	public ImageDescriptor getDeleteImageDescriptorFor(Class<?> type) {
		return defaultImageDescriptors.get(type);
	}	

	public ImageDescriptor getUpdateImageDescriptorFor(Class<?> type) {
		return updateImageDescriptors.get(type);
	}	

}
