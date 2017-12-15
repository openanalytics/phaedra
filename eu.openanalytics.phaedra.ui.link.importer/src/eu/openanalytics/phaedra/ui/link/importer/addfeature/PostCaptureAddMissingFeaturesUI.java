package eu.openanalytics.phaedra.ui.link.importer.addfeature;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.hook.BaseHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask.DataCaptureParameter;
import eu.openanalytics.phaedra.datacapture.hook.DataCaptureHookArguments;
import eu.openanalytics.phaedra.datacapture.store.IDataCaptureStore;
import eu.openanalytics.phaedra.datacapture.util.MissingFeaturesHelper;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class PostCaptureAddMissingFeaturesUI extends BaseHook {

	@Override
	public void post(IHookArguments args) {
		// This hook is only for UI (non-server) instances.
		if (DataCaptureService.getInstance().isServerEnabled()) return;
		
		DataCaptureHookArguments dcArgs = (DataCaptureHookArguments) args;
		IDataCaptureStore store = dcArgs.context.getStore(dcArgs.reading);
		ProtocolClass pClass = ProtocolUtils.getProtocolClass(dcArgs.plate);
	
		Boolean createMissing = (Boolean) dcArgs.context.getTask().getParameters().get(DataCaptureParameter.CreateMissingWellFeatures.name());
		if (createMissing != null && createMissing.booleanValue()) {
			createMissing(new MissingFeaturesHelper(store, pClass, Feature.class));
		}
		
		createMissing = (Boolean) dcArgs.context.getTask().getParameters().get(DataCaptureParameter.CreateMissingSubWellFeatures.name());
		if (createMissing != null && createMissing.booleanValue()) {
			createMissing(new MissingFeaturesHelper(store, pClass, SubWellFeature.class));
		}
	}

	private void createMissing(MissingFeaturesHelper helper) {
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
	}
}
