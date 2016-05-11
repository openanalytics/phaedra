package eu.openanalytics.phaedra.ui.link.importer.addfeature;

import org.eclipse.jface.window.Window;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.link.data.addfeatures.AddFeaturesTask;
import eu.openanalytics.phaedra.link.data.addfeatures.AddFeaturesUtils;

public class AddFeaturesWizard extends BaseStatefulWizard {
	
	public AddFeaturesWizard(AddFeaturesWizardState preconfiguredState) {
		setWindowTitle("Add New Features");
		state = preconfiguredState;
	}
	
	@Override
	public void addPages() {
		addPage(new NewFeaturesPage());
	}
	
	@Override
	public boolean canFinish() {
		return true;
	}

	@Override
	public boolean performFinish() {
		super.performFinish();
		
		AddFeaturesTask task = ((AddFeaturesWizardState)state).task;
		int retVal = AddFeaturesUtils.promptFeatureCreation(task);
		
		switch (retVal) {
		case Window.OK:
			// Create features, close wizard
			AddFeaturesUtils.createMissingFeatures(task);
			return true;
		case Window.CANCEL:
			// Create nothing, close wizard
			return true;
		default:
			// Create nothing, do not close wizard
			return false;
		}
	}

	public static class AddFeaturesWizardState implements IWizardState {
		public AddFeaturesTask task= new AddFeaturesTask();
	}
}
