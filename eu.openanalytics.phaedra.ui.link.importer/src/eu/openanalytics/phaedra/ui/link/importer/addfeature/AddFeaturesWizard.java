package eu.openanalytics.phaedra.ui.link.importer.addfeature;

import java.util.List;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.datacapture.util.FeatureDefinition;
import eu.openanalytics.phaedra.datacapture.util.MissingFeaturesHelper;

public class AddFeaturesWizard extends BaseStatefulWizard {
	
	private AddFeaturesWizardState state;
	
	public AddFeaturesWizard(MissingFeaturesHelper helper) {
		setWindowTitle("Add New Features");
		state = new AddFeaturesWizardState();
		state.helper = helper;
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
		if (state.featureDefinitions == null || state.featureDefinitions.isEmpty()) return true;
		if (state.helper.confirmFeatureCreation(state.featureDefinitions)) {
			state.helper.createMissingFeatures(state.featureDefinitions);
		}
		return true;
	}

	public static class AddFeaturesWizardState implements IWizardState {
		public MissingFeaturesHelper helper;
		public List<FeatureDefinition> featureDefinitions;
	}
}
