package eu.openanalytics.phaedra.ui.link.importer.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class ImportWizardRegistry {

	private static ImportWizardRegistry instance;
	
	private List<WizardDescriptor> wizardDescriptors;
	
	private ImportWizardRegistry() {
		// Hidden constructor
		loadWizards();
	}
	
	public static ImportWizardRegistry getInstance() {
		if (instance == null) instance = new ImportWizardRegistry();
		return instance;
	}
	
	public WizardDescriptor[] getImportWizards() {
		return wizardDescriptors.toArray(new WizardDescriptor[wizardDescriptors.size()]);
	}
	
	private void loadWizards() {
		wizardDescriptors = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(WizardDescriptor.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			WizardDescriptor descriptor = new WizardDescriptor(el);
			wizardDescriptors.add(descriptor);
		}
	}
}
