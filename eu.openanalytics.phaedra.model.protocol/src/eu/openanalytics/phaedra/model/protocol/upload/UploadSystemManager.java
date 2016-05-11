package eu.openanalytics.phaedra.model.protocol.upload;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class UploadSystemManager {

	private static UploadSystemManager instance =  new UploadSystemManager();
	
	private List<IUploadSystem> systems;
	private ResourceManager iconCache;
	
	private UploadSystemManager() {
		iconCache = new LocalResourceManager(JFaceResources.getResources(Display.getDefault()));
		systems = new ArrayList<>();
		loadSystems();
	}
	
	public static UploadSystemManager getInstance() {
		return instance;
	}
	
	public List<IUploadSystem> getSystems() {
		return new ArrayList<>(systems);
	}
	
	public IUploadSystem getSystem(String systemName) {
		for (IUploadSystem system: systems) {
			if (system.getName().equalsIgnoreCase(systemName)) return system;
		}
		return null;
	}
	
	public Image getIcon(String systemName) {
		IUploadSystem system = getSystem(systemName);
		if (system == null) return null;
		return (Image) iconCache.get(system.getIcon());
	}
	
	private void loadSystems() {
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IUploadSystem.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				IUploadSystem system = (IUploadSystem) el.createExecutableExtension(IUploadSystem.ATTR_CLASS);
				systems.add(system);
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}
}
