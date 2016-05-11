package eu.openanalytics.phaedra.ui.link.importer.wizard;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import eu.openanalytics.phaedra.ui.link.importer.Activator;

public class WizardDescriptor {

	public final static String EXT_PT_ID = Activator.PLUGIN_ID + ".importWizard";
	public final static String ATTR_NAME = "name";
	public final static String ATTR_ICON = "icon";
	public final static String ATTR_DESCRIPTION = "description";
	public final static String ATTR_CLASS = "class";
	
	private String name;
	private ImageDescriptor icon;
	private String description;
	
	private IConfigurationElement config;
	
	public WizardDescriptor(IConfigurationElement config) {
		this.config = config;
		this.name = config.getAttribute(ATTR_NAME);
		this.description = config.getAttribute(ATTR_DESCRIPTION);
		
		String iconName = config.getAttribute(ATTR_ICON);
		if (iconName != null) {
			IExtension extension = config.getDeclaringExtension();
	        String extendingPluginId = extension.getNamespaceIdentifier();
	        this.icon = AbstractUIPlugin.imageDescriptorFromPlugin(extendingPluginId, iconName);
		} else {
			this.icon = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/import_small.png");
		}
	}
	
	public String getName() {
		return name;
	}
	
	public ImageDescriptor getIcon() {
		return icon;
	}
	
	public String getDescription() {
		return description;
	}
	
	public IWizard createWizard() throws CoreException {
		return (IWizard)config.createExecutableExtension(ATTR_CLASS);
	}
}
