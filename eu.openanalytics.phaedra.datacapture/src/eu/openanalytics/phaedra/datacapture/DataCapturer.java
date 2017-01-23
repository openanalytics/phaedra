package eu.openanalytics.phaedra.datacapture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.google.common.collect.Lists;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.module.IModule;
import eu.openanalytics.phaedra.datacapture.module.ModuleFactory;
import eu.openanalytics.phaedra.datacapture.store.IDataCaptureStore;

public class DataCapturer {

	public List<PlateReading> execute(DataCaptureTask task, IProgressMonitor monitor) throws DataCaptureException {
		return executeInternal(task, monitor);
	}
	
	private List<PlateReading> executeInternal(DataCaptureTask task, IProgressMonitor monitor) throws DataCaptureException {
		
		monitor.beginTask("Capturing data", 100);
		
		DataCaptureContext ctx = new DataCaptureContext(task);
		ctx.getLogger().started(task);
		
		try {
			monitor.subTask("Checking task configuration");
			verifyTask(task);
			
			if (monitor.isCanceled()) doCancel(false, ctx, task, monitor);
			
			IModule[] modules = null;
			try {
				modules = ModuleFactory.createAndConfigureModules(task);
			} catch (IOException e) {
				throw new DataCaptureException("Failed to load modules", e);
			}
			
			if (monitor.isCanceled()) doCancel(false, ctx, task, monitor);

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
					if (monitor.isCanceled()) return doCancel(true, ctx, task, monitor);
					monitor.subTask("Executing module: " + module.getName());
					int progress = (int)((module.getWeight()/totalWeight) * availableProgress);
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, progress);
					ctx.setActiveModule(module);
					module.execute(ctx, subMonitor);
					subMonitor.done();
				}
			} finally {
				// Remember which module caused an error (if any).
				IModule lastExecutedModule = ctx.getActiveModule();
				
				// Perform post-capture steps, regardless of the outcome of the capture.
				int progressPerModule = 5 / modulesToExecute.size();
				for (IModule module: modulesToExecute) {
					try {
						SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, progressPerModule);
						ctx.setActiveModule(module);
						module.postCapture(ctx, subMonitor);
						subMonitor.done();
					} catch (Throwable t) {
						ctx.getLogger().warn("Post-process of module '" + module.getId() + "' failed: " + t.getMessage());
					} finally {
						ctx.setActiveModule(lastExecutedModule);
					}
				}
			}
			
			if (monitor.isCanceled()) return doCancel(true, ctx, task, monitor);
			
			monitor.subTask("Uploading captured data");
			ctx.setActiveModule(null);
			for (PlateReading reading: ctx.getReadings()) {
				ctx.getLogger().info(reading, "Uploading reading");
				if (task.isTest()) {
					ctx.getStore(reading).rollback();
					// Generate a pseudo id for test readings (see PlateReading.equals())
					long id = 10000000L + (long)(Math.random()*10000000);
					reading.setId(id);
				} else {
					ctx.getStore(reading).finish();
					
					// If target protocol and experiment are known, save them into the reading.
					Object protocolName = task.getParameters().get(DataCaptureTask.PARAM_PROTOCOL_NAME);
					Object experimentName = task.getParameters().get(DataCaptureTask.PARAM_EXPERIMENT_NAME);
					if (protocolName != null) reading.setProtocol(protocolName.toString());
					if (experimentName != null) reading.setExperiment(experimentName.toString());
					
					DataCaptureService.getInstance().updateReading(reading);
					ctx.getLogger().completed(reading, ctx.getReadingSourceId(reading));
				}
			}
			monitor.worked(20);
			
			ctx.setActiveModule(null);
			ctx.getLogger().completed(task);
			monitor.done();
			
			// Return the captured readings.
			return Lists.newArrayList(ctx.getReadings());
			
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
		return Lists.newArrayList();
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
}
