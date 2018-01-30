package eu.openanalytics.phaedra.datacapture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import eu.openanalytics.phaedra.base.environment.Screening;
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
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

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

			// Find out if any modules have to be skipped.
			List<IModule> modulesToExecute = new ArrayList<>();
			String[] moduleFilter = task.getModuleFilter();
			for (int i=0; i<modules.length; i++) {
				if (moduleFilter == null  || CollectionUtils.find(moduleFilter, modules[i].getId()) != -1) {
					modulesToExecute.add(modules[i]);
				}
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
			} finally {
				// Remember which module caused an error (if any).
				IModule lastExecutedModule = ctx.getActiveModule();
				
				// Perform post-capture steps, regardless of the outcome of the capture.
				int progressPerModule = 5 / modulesToExecute.size();
				for (IModule module: modulesToExecute) {
					try {
						ctx.setActiveModule(module);
						module.postCapture(ctx, mon.split(progressPerModule));
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
				ctx.getLogger().info(reading, "Uploading reading");
				finish(reading, ctx);
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
		Experiment experiment = (Experiment) task.getParameters().get(DataCaptureParameter.TargetExperiment.name());
		
		if (experiment == null) {
			// Create a new experiment, or find the experiment with matching name.
			
			String experimentName = (String) task.getParameters().get(DataCaptureParameter.TargetExperimentName.name());
			Protocol protocol = (Protocol) task.getParameters().get(DataCaptureParameter.TargetProtocol.name());
			if (protocol == null || experimentName == null) throw new DataCaptureException("Cannot create new experiment: target protocol and/or experiment name not set");
			
			Screening.getEnvironment().getEntityManager().refresh(protocol);
			experiment = PlateService.getInstance().getExperiments(protocol).stream()
					.map(e -> { Screening.getEnvironment().getEntityManager().refresh(e); return e; })
					.filter(e -> e.getName().equalsIgnoreCase(experimentName))
					.findAny()
					.orElse(createNewExperiment(experimentName, protocol, task.getUser()));
		}
		
		//TODO Import to existing plates is not supported
		int sequence = 1 + PlateService.getInstance().getPlates(experiment).stream().mapToInt(p -> p.getSequence()).max().orElse(0);
		Plate plate = createNewPlate(reading, sequence, experiment);
		
		reading.setLinkStatus(-1);
		reading.setLinkDate(new Date());
		reading.setLinkUser(SecurityService.getInstance().getCurrentUserName());
		reading.setProtocol(experiment.getProtocol().getName());
		reading.setExperiment(experiment.getName());
		
		try {
			DataCaptureHookManager.postCapture(ctx, reading, plate);
			ctx.getStore(reading).finish(plate);
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
}
