package eu.openanalytics.phaedra.link.data.addfeatures;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.hook.IBatchedHook;
import eu.openanalytics.phaedra.base.hook.IBatchedHookArguments;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.util.FeatureDefinition;
import eu.openanalytics.phaedra.link.data.DataLinkTask;
import eu.openanalytics.phaedra.link.data.hook.LinkDataBatchedHookArguments;
import eu.openanalytics.phaedra.link.data.hook.LinkDataHookArguments;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

/**
 * Pre link hook: detect new features and ask to add them in the protocol class.
 */
public class PreLinkHeadlessHook implements IBatchedHook {

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
		// If running in client mode, another hook (PreLinkHook) will take care of this.
		if (!DataCaptureService.getInstance().isServerEnabled()) return;

		ProtocolClass pClass = null;
		List<PlateReading> readings = new ArrayList<>();
		DataLinkTask task = null;
		
		if (args instanceof LinkDataHookArguments) {
			LinkDataHookArguments linkArgs = (LinkDataHookArguments)args;
			pClass = linkArgs.task.targetExperiment.getProtocol().getProtocolClass();
			readings.add(linkArgs.reading);
			task = linkArgs.task;
		} else if (args instanceof LinkDataBatchedHookArguments) {
			LinkDataBatchedHookArguments linkArgs = (LinkDataBatchedHookArguments)args;
			pClass = linkArgs.task.targetExperiment.getProtocol().getProtocolClass();
			readings = linkArgs.readings;
			task = linkArgs.task;
		}
		
		if (task.createMissingWellFeatures) detectFeatures(pClass, readings, Feature.class);
		if (task.createMissingSubWellFeatures) detectFeatures(pClass, readings, SubWellFeature.class);
	}
	
	private void detectFeatures(ProtocolClass protocolClass, List<PlateReading> readings, Class<? extends IFeature> featureClass) {
		AddFeaturesTask task = new AddFeaturesTask();
		task.targetProtocolClass = protocolClass;
		task.featureClass = featureClass;
		task.featureDefinitions = AddFeaturesUtils.calculateMissingFeatures(task, readings);
		
		if (task.featureDefinitions.isEmpty()) return;
		// Shouldn't happen: the server account has full access.
		if (!ProtocolService.getInstance().canEditProtocolClass(protocolClass)) return;
		
		// Add ALL missing features.
		for (FeatureDefinition def: task.featureDefinitions) def.addFeatureToProtocolClass = true;
		AddFeaturesUtils.createMissingFeatures(task);
	}
}
