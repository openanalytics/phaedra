package eu.openanalytics.phaedra.datacapture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import eu.openanalytics.phaedra.base.environment.GenericEntityService;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask.DataCaptureParameter;
import eu.openanalytics.phaedra.datacapture.hook.DataCaptureHookManager;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.module.IModule;
import eu.openanalytics.phaedra.datacapture.module.ModuleFactory;
import eu.openanalytics.phaedra.datacapture.store.IDataCaptureStore;
import eu.openanalytics.phaedra.datacapture.util.MissingFeaturesHelper;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class DataCapturer {

	public List<PlateReading> execute(DataCaptureTask task, IProgressMonitor monitor) throws DataCaptureException {
		return executeInternal(task, monitor);
	}
	
	private List<PlateReading> executeInternal(DataCaptureTask task, IProgressMonitor monitor) throws DataCaptureException {
		SubMonitor mon = SubMonitor.convert(monitor);
		mon.beginTask("Capturing data", 100);
		
		DataCaptureContext ctx = new DataCaptureContext(task);
		ctx.getLogger().started(task);
		DataCaptureHookManager.preCapture(ctx);
		
		try {
			mon.subTask("Checking task configuration");
			verifyTask(task);
			
			if (mon.isCanceled()) doCancel(false, ctx, task, mon);
			
			IModule[] modules = null;
			try {
				modules = ModuleFactory.createAndConfigureModules(task);
			} catch (IOException e) {
				throw new DataCaptureException("Failed to load modules", e);
			}
			
			if (mon.isCanceled()) doCancel(false, ctx, task, mon);

			List<IModule> modulesToExecute = new ArrayList<>();
			
			IModule[] preModules = (IModule[]) task.getParameters().get(DataCaptureParameter.PreModules.name());
			if (preModules != null && preModules.length > 0) {
				for (IModule preModule: preModules) {
					if (modules.length > 0) preModule.getConfig().setParentConfig(modules[0].getConfig().getParentConfig());
					modulesToExecute.add(preModule);
				}
			}
			
			// Find out if any modules have to be skipped.
			String[] moduleFilter = task.getModuleFilter();
			for (int i=0; i<modules.length; i++) {
				if (moduleFilter == null  || CollectionUtils.find(moduleFilter, modules[i].getId()) != -1) {
					modulesToExecute.add(modules[i]);
				}
			}
			
			IModule[] postModules = (IModule[]) task.getParameters().get(DataCaptureParameter.PostModules.name());
			if (postModules != null && postModules.length > 0) {
				for (IModule postModule: postModules) modulesToExecute.add(postModule);
			}
			
			double totalWeight = modulesToExecute.stream().mapToInt(m -> m.getWeight()).sum();
			if (totalWeight == 0) totalWeight = 1;
			int availableProgress = 75;
			
			try {
				for (IModule module: modulesToExecute) {
					if (mon.isCanceled()) return doCancel(true, ctx, task, mon);
					mon.subTask("Executing module: " + module.getName());
					
					int progressPerModule = (int)((module.getWeight()/totalWeight) * availableProgress);
					ctx.setActiveModule(module);
					module.execute(ctx, mon.split(progressPerModule));
				}
			} catch (Throwable t) {
				ctx.getTask().getParameters().put("DataCaptureException", t);
				throw t;
			} finally {
				// Remember which module caused an error (if any).
				IModule lastExecutedModule = ctx.getActiveModule();
				
				// Perform post-capture steps, regardless of the outcome of the capture.
				int progressPerModule = 5 / modulesToExecute.size();
				for (IModule module: modulesToExecute) {
					try {
						ctx.setActiveModule(module);
						module.postCapture(ctx, mon.isCanceled() ? new NullProgressMonitor() : mon.split(progressPerModule));
					} catch (Throwable t) {
						ctx.getLogger().warn("Post-process of module '" + module.getId() + "' failed: " + t.getMessage());
					} finally {
						ctx.setActiveModule(lastExecutedModule);
					}
				}
			}
			
			if (mon.isCanceled()) return doCancel(true, ctx, task, mon);
			
			mon.subTask("Uploading captured data");
			ctx.setActiveModule(null);
			for (PlateReading reading: ctx.getReadings()) {
				if (task.isTest()) {
					//TODO Store sourceId in fileName and use a pseudo id, for plateMapping (see finish() below)
					reading.setFileName(ctx.getReadingSourceId(reading));
					reading.setId(100000000L + (long)(Math.random()*10000000));
					ctx.getStore(reading).rollback();
				} else {
					ctx.getLogger().info(reading, "Uploading reading");
					finish(reading, ctx);
				}
			}
			mon.worked(20);
			
			ctx.setActiveModule(null);
			ctx.getLogger().completed(task);
			return Arrays.stream(ctx.getReadings()).collect(Collectors.toList());
			
		} catch (Throwable e) {
			rollback(ctx, task, monitor);
			ctx.getLogger().error(null, "Data capture stopped (due to error)", e);
			if (e instanceof DataCaptureException) throw (DataCaptureException)e;
			throw new DataCaptureException("Data capture failed: " + e.getMessage(), e);
		}
	}
	
	private List<PlateReading> doCancel(boolean rollbackRequired, DataCaptureContext ctx, DataCaptureTask task, IProgressMonitor monitor) {
		if (rollbackRequired) rollback(ctx, task, monitor);
		ctx.getLogger().cancelled(task);
		return new ArrayList<>();
	}
	
	private void rollback(DataCaptureContext ctx, DataCaptureTask task, IProgressMonitor monitor) {
		monitor.subTask("Cleaning up temporary objects");
		for (PlateReading reading: ctx.getReadings()) {
			IDataCaptureStore store = ctx.getStore(reading);
			if (store != null) store.rollback();
		}
	}
	
	private void verifyTask(DataCaptureTask task) throws DataCaptureException {
		if (task == null || task.getConfigId() == null || task.getConfigId().isEmpty()) {
			throw new DataCaptureException("No capture configuration specified");
		}
	}
	
	private void finish(PlateReading reading, DataCaptureContext ctx) throws DataCaptureException {
		DataCaptureTask task = ctx.getTask();
		
		Experiment experiment = (Experiment) ctx.getParameters(reading).getParameter(DataCaptureParameter.TargetExperiment.name());
		if (experiment == null) experiment = (Experiment) task.getParameters().get(DataCaptureParameter.TargetExperiment.name());
		
		if (experiment == null) {
			// Create a new experiment, or find the experiment with matching name.
			
			String experimentName = (String) task.getParameters().get(DataCaptureParameter.TargetExperimentName.name());
			Protocol protocol = (Protocol) task.getParameters().get(DataCaptureParameter.TargetProtocol.name());
			if (protocol == null || experimentName == null) throw new DataCaptureException("Cannot create new experiment: target protocol and/or experiment name not set");
			
			GenericEntityService.getInstance().refreshEntity(protocol);
			List<Experiment> experiments = PlateService.getInstance().getExperiments(protocol);
			for (Experiment e: experiments) {
				GenericEntityService.getInstance().refreshEntity(e);
				if (e.getName().equalsIgnoreCase(experimentName)) { experiment = e; break; }
			}
			if (experiment == null) experiment = createNewExperiment(experimentName, protocol, task.getUser());
		}
		
		@SuppressWarnings("unchecked")
		Map<PlateReading, Plate> plateMapping = (Map<PlateReading, Plate>) task.getParameters().get(DataCaptureParameter.PlateMapping.name());
		
		Plate plate = null;
		if (plateMapping == null) {
			int sequence = 1 + PlateService.getInstance().getPlates(experiment).stream().mapToInt(p -> p.getSequence()).max().orElse(0);
			plate = createNewPlate(reading, sequence, experiment);
		} else {
			String sourceId = ctx.getReadingSourceId(reading);
			PlateReading key = plateMapping.keySet().stream().filter(r -> sourceId.equalsIgnoreCase(r.getFileName())).findAny().orElse(null);
			if (key == null) throw new DataCaptureException("Cannot find plate: reading id mismatch");
			plate = plateMapping.get(key);
		}
		
		reading.setLinkStatus(-1);
		reading.setLinkDate(new Date());
		reading.setLinkUser(SecurityService.getInstance().getCurrentUserName());
		reading.setProtocol(experiment.getProtocol().getName());
		reading.setExperiment(experiment.getName());
		
		try {
			checkMissingFeatures(ctx, reading, plate);
			ctx.getStore(reading).finish(plate);
			DataCaptureHookManager.postCapture(ctx, reading, plate);
			reading.setLinkStatus(1);
			ctx.getLogger().completed(reading, ctx.getReadingSourceId(reading));
		} finally {
			DataCaptureService.getInstance().updateReading(reading);
		}
		
		CalculationService.getInstance().calculate(plate);
	}
	
	private Experiment createNewExperiment(String name, Protocol parent, String userName) {
		Experiment experiment = PlateService.getInstance().createExperiment(parent);
		experiment.setName(name);
		experiment.setCreator(userName);
		try {
			PlateService.getInstance().updateExperiment(experiment);
		} catch (Throwable t) {
			EclipseLog.error("Cannot auto-link reading: failed to create experiment '" + name + "'", t, Activator.getDefault());
			return null;
		}
		return experiment;
	}
	
	private Plate createNewPlate(PlateReading reading, int sequence, Experiment experiment) {
		int rows = reading.getRows();
		rows = rows == 0 ? 16 : rows;
		int cols = reading.getColumns();
		cols = cols == 0 ? 24 : cols;
		
		if (reading.getFileInfo() != null && NumberUtils.isNumeric(reading.getFileInfo())) {
			sequence = Integer.parseInt(reading.getFileInfo());
		}
		
		Plate plate = PlateService.getInstance().createPlate(experiment, rows, cols);
		plate.setSequence(sequence);
		plate.setBarcode(reading.getBarcode());
		
		PlateService.getInstance().updatePlate(plate);
		return plate;
	}
	
	private void checkMissingFeatures(DataCaptureContext ctx, PlateReading reading, Plate plate) throws DataCaptureException {
		IDataCaptureStore store = ctx.getStore(reading);
		ProtocolClass pClass = ProtocolUtils.getProtocolClass(plate);
		
		Boolean createMissing = (Boolean) ctx.getTask().getParameters().get(DataCaptureParameter.CreateMissingWellFeatures.name());
		if (createMissing != null && createMissing.booleanValue()) {
			new MissingFeaturesHelper(store, pClass, Feature.class).createMissingFeatures();
		}
		
		createMissing = (Boolean) ctx.getTask().getParameters().get(DataCaptureParameter.CreateMissingSubWellFeatures.name());
		if (createMissing != null && createMissing.booleanValue()) {
			new MissingFeaturesHelper(store, pClass, SubWellFeature.class).createMissingFeatures();
		}
	}
}
