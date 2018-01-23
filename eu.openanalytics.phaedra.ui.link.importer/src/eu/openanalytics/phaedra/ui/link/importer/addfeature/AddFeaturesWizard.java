package eu.openanalytics.phaedra.ui.link.importer.addfeature;

import java.util.List;

import eu.openanalytics.phaedra.base.ui.util.wizard.BaseStatefulWizard;
import eu.openanalytics.phaedra.base.ui.util.wizard.IWizardState;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.util.FeatureDefinition;
import eu.openanalytics.phaedra.datacapture.util.MissingFeaturesHelper;
import eu.openanalytics.phaedra.ui.link.importer.Activator;

public class AddFeaturesWizard extends BaseStatefulWizard {
	
	public AddFeaturesWizard(MissingFeaturesHelper helper) {
		setWindowTitle("Add New Features");
		state = new AddFeaturesWizardState();
		((AddFeaturesWizardState) state).helper = helper;
		try {
			((AddFeaturesWizardState) state).featureDefinitions = helper.findMissingFeatures();
		} catch (DataCaptureException e) {
			EclipseLog.warn("Failed to detect missing features", e, Activator.PLUGIN_ID);
		}
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
		AddFeaturesWizardState s = (AddFeaturesWizardState) state;
		if (s.featureDefinitions == null || s.featureDefinitions.isEmpty()) return true;
		if (s.helper.confirmFeatureCreation(s.featureDefinitions)) {
			s.helper.createMissingFeatures(s.featureDefinitions);
		}
		return true;
	}

	public static class AddFeaturesWizardState implements IWizardState {
		public MissingFeaturesHelper helper;
		public List<FeatureDefinition> featureDefinitions;
	}
}
