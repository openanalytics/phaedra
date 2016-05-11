package eu.openanalytics.phaedra.base.ui.icons;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

public class IconManager {

	private final static String DEFAULT_ICON_PATH = "icons/";
	
	private static Map<String, Image> iconCache = new HashMap<String, Image>();

	public static Image getIconImage(String iconName) {

		if (iconName == null)
			return null;

		if (iconCache.containsKey(iconName)) {
			Image img = iconCache.get(iconName);

			if (img != null && img.isDisposed()) {
				iconCache.remove(iconName);
				img = getIconImage(iconName);
			}

			return img;
		}

		ImageDescriptor desc = getIconDescriptor(iconName);
		if (desc == null)
			return null;

		Image image = desc.createImage();

		if (image != null) {
			iconCache.put(iconName, image);
		}

		return image;
	}

	public static ImageDescriptor getIconDescriptor(String imageName) {
		return getIconDescriptor(imageName, DEFAULT_ICON_PATH);
	}

	public static ImageDescriptor getIconDescriptor(String imageName, String path) {
		Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
		Path imagePath = new Path(path + imageName);
		URL imageUrl = FileLocator.find(bundle, imagePath, null);
		if (imageUrl == null)
			return null;
		ImageDescriptor desc = ImageDescriptor.createFromURL(imageUrl);
		return desc;
	}
	
	public static ImageDescriptor getDefaultIconDescriptor(Class<?> type) {
		return IconRegistry.getInstance().getDefaultImageDescriptorFor(type);
	}
	
	public static ImageDescriptor getCreateIconDescriptor(Class<?> type) {
		return IconRegistry.getInstance().getCreateImageDescriptorFor(type);
	}

	public static ImageDescriptor getDeleteIconDescriptor(Class<?> type) {
		return IconRegistry.getInstance().getDeleteImageDescriptorFor(type);
	}
	
	public static ImageDescriptor getUpdateIconDescriptor(Class<?> type) {
		return IconRegistry.getInstance().getUpdateImageDescriptorFor(type);
	}

}
