package eu.openanalytics.phaedra.datacapture.hook;

import eu.openanalytics.phaedra.base.hook.BaseHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask.DataCaptureParameter;
import eu.openanalytics.phaedra.datacapture.store.IDataCaptureStore;
import eu.openanalytics.phaedra.datacapture.util.MissingFeaturesHelper;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class PostCaptureAddMissingFeaturesHeadless extends BaseHook {

	@Override
	public void post(IHookArguments args) {
		// This hook is only for headless (server) instances.
		if (!DataCaptureService.getInstance().isServerEnabled()) return;
		
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
		try {
			helper.createMissingFeatures(helper.findMissingFeatures());
		} catch (DataCaptureException e) {
			EclipseLog.error("Failed to detect missing features", e, Activator.PLUGIN_ID);
		}
	}
}
