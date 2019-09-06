package eu.openanalytics.phaedra.ui.link.importer.addfeature;

import java.util.List;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.util.FeatureDefinition;
import eu.openanalytics.phaedra.datacapture.util.IMissingFeaturesHandler;
import eu.openanalytics.phaedra.datacapture.util.MissingFeaturesHelper;

public class UIMissingFeaturesHandler implements IMissingFeaturesHandler {

	@Override
	public boolean handle(List<FeatureDefinition> missingFeatures, MissingFeaturesHelper helper) {
		// If DC server is running, abort.
		if (DataCaptureService.getInstance().isServerEnabled()) return false;
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				AddFeaturesWizard wizard = new AddFeaturesWizard(helper);
				WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
				dialog.setPageSize(800, 500);
				dialog.create();
				dialog.open();
			}
		});
		
		return true;
	}

}
