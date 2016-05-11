package eu.openanalytics.phaedra.ui.link.importer.addfeature;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.hook.IBatchedHook;
import eu.openanalytics.phaedra.base.hook.IBatchedHookArguments;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.addfeatures.AddFeaturesUtils;
import eu.openanalytics.phaedra.link.data.hook.LinkDataBatchedHookArguments;
import eu.openanalytics.phaedra.link.data.hook.LinkDataHookArguments;
import eu.openanalytics.phaedra.link.importer.Activator;
import eu.openanalytics.phaedra.link.importer.preferences.Prefs;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.ui.link.importer.addfeature.AddFeaturesWizard.AddFeaturesWizardState;

/**
 * Pre link hook: detect new features and ask to add them in the protocol class.
 */
public class PreLinkHook implements IBatchedHook {

	private boolean batchSupported = false;
	
	@Override
	public void pre(IHookArguments args) throws PreHookException {
		if (!batchSupported) {
			detectFeatures(args);
		}
	}

	@Override
	public void post(IHookArguments args) {
		// Do nothing
	}

	@Override
	public void startBatch(IBatchedHookArguments args) {
		batchSupported = true;
		detectFeatures(args);
	}

	@Override
	public void endBatch(boolean successful) {
		// Do nothing
	}
	
	private void detectFeatures(Object args) {
		// If running in server mode, another hook (PreLinkHeadlessHook) will take care of this.
		if (DataCaptureService.getInstance().isServerEnabled()) return;
		
		ProtocolClass pClass = null;
		List<PlateReading> readings = new ArrayList<>();
		if (args instanceof LinkDataHookArguments) {
			LinkDataHookArguments linkArgs = (LinkDataHookArguments)args;
			pClass = linkArgs.task.targetExperiment.getProtocol().getProtocolClass();
			readings.add(linkArgs.reading);
		} else if (args instanceof LinkDataBatchedHookArguments) {
			LinkDataBatchedHookArguments linkArgs = (LinkDataBatchedHookArguments)args;
			pClass = linkArgs.task.targetExperiment.getProtocol().getProtocolClass();
			readings = linkArgs.readings;
		}
		boolean detectWellFeatures = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.DETECT_WELL_FEATURES);
		boolean detectSubWellFeatures = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.DETECT_SUBWELL_FEATURES);
		if (detectWellFeatures) detectFeatures(pClass, readings, Feature.class);
		if (detectSubWellFeatures) detectFeatures(pClass, readings, SubWellFeature.class);
	}
	
	private void detectFeatures(ProtocolClass protocolClass, List<PlateReading> readings, Class<? extends IFeature> featureClass) {
		AddFeaturesWizardState state = new AddFeaturesWizardState();
		state.task.targetProtocolClass = protocolClass;
		state.task.featureClass = featureClass;
		state.task.featureDefinitions = AddFeaturesUtils.calculateMissingFeatures(state.task, readings);
		
		if (state.task.featureDefinitions.isEmpty()) return;
		
		if (!ProtocolService.getInstance().canEditProtocolClass(protocolClass)) {
			String protocolClassName = protocolClass.getName();
			String featureType = (state.task.featureClass == Feature.class) ? "well" : "subwell";
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openWarning(null, "Missing " + featureType + " features",
							"The import contains " + featureType + " features are not present in protocol class " + protocolClassName + "."
							+ " These features cannot be added automatically, because you do not have permission to edit the protocol class."
							+ " Please contact an administrator. The import will proceed without adding the missing features.");
				}
			});
			return;
		}
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				AddFeaturesWizard wizard = new AddFeaturesWizard(state);
				WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
				dialog.setPageSize(800, 500);
				dialog.create();
				dialog.open();
			}
		});
	}
}
