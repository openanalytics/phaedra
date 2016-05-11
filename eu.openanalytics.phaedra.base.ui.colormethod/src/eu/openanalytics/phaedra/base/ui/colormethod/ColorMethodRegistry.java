package eu.openanalytics.phaedra.base.ui.colormethod;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.ui.colormethod.minmeanmax.MinMeanMaxColorMethod;
import eu.openanalytics.phaedra.base.util.CollectionUtils;

public class ColorMethodRegistry {

	private String[] ids;
	private String[] names;
	
	private static ColorMethodRegistry instance;
	
	private ColorMethodRegistry() {
		// Hidden constructor.
		loadMethods();
	}
	
	public static ColorMethodRegistry getInstance() {
		if (instance == null) instance = new ColorMethodRegistry();
		return instance;
	}
	
	public String[] getIds() {
		return ids;
	}
	
	public String[] getNames() {
		return names;
	}
	
	public String getName(String id) {
		int index = CollectionUtils.find(ids, id);
		if (index == -1) return null;
		return names[index];
	}
	
	public IColorMethod createMethod(String id) {
		return loadColorMethod(id);
	}
	
	public IColorMethod getDefaultColorMethod() {
		return createMethod(MinMeanMaxColorMethod.class.getName());
	}
	
	private void loadMethods() {
		List<String> ids = new ArrayList<String>();
		List<String> names = new ArrayList<String>();
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IColorMethod.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			ids.add(el.getAttribute(IColorMethod.ATTR_ID));
			names.add(el.getAttribute(IColorMethod.ATTR_NAME));
		}
		
		this.ids =  ids.toArray(new String[ids.size()]);
		this.names =  names.toArray(new String[names.size()]);
	}
	
	private IColorMethod loadColorMethod(String id) {
		IConfigurationElement[] config = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(IColorMethod.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				String methodId = el.getAttribute(IColorMethod.ATTR_ID);
				if (id.equals(methodId)) {
					Object o = el.createExecutableExtension(IColorMethod.ATTR_CLASS);
					return (IColorMethod)o;
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
		return null;
	}
}
