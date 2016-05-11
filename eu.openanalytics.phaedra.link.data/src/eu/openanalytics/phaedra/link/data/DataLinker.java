package eu.openanalytics.phaedra.link.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.hook.LinkDataHookManager;
import eu.openanalytics.phaedra.link.data.image.ImageDataLinker;
import eu.openanalytics.phaedra.link.data.plate.PlateDataLinker;
import eu.openanalytics.phaedra.link.data.subwell.SubWellDataLinker;
import eu.openanalytics.phaedra.link.data.welldata.WellDataLinker;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class DataLinker {

	private DataLinkTask task;
	
	public void setTask(DataLinkTask task) {
		this.task = task;
	}
	
	public IStatus execute(IProgressMonitor monitor) {
		
		// First, a security check.
		boolean access = false;
		if (task.createNewPlates) {
			access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, task.targetExperiment);
		} else {
			for (Plate p: task.mappedReadings.values()) {
				access = SecurityService.getInstance().checkWithDialog(Permissions.PLATE_EDIT, p);
				if (!access) break;
			}
		}
		if (!access) return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Permission Denied");

		List<PlateReading> readings = new ArrayList<PlateReading>();
		if (task.createNewPlates) {
			// Link all selected readings.
			readings = task.selectedReadings;
		} else {
			// Link only the mapped readings.
			readings.addAll(task.mappedReadings.keySet());
		}
		
		monitor.beginTask("Linking " + readings.size() + " plate reading(s)", 100);
		try {
			int progressPerPlate = 100 / readings.size();
			
			int sequence = 1;
			if (task.createNewPlates) {
				// Offset sequence nr if the plate is added to an existing experiment.
				sequence = 1 + PlateService.getInstance().getPlates(task.targetExperiment).stream()
						.mapToInt(p -> p.getSequence()).max().orElse(0);
			}
			
			LinkDataHookManager.startLinkBatch(task, readings);
			for (PlateReading reading: readings) {
				
				if (monitor.isCanceled()) {
					rollback(monitor);
					return Status.CANCEL_STATUS;
				}
				
				monitor.subTask("Linking reading " + reading.getBarcode());
				
				LinkDataHookManager.preLink(task, reading);
				Plate plate = doLinkData(reading, sequence++, monitor);
				LinkDataHookManager.postLink(task, reading, plate);
				
				monitor.worked(progressPerPlate);
			}
			LinkDataHookManager.endLinkBatch(true);
			monitor.done();
			
		} catch (Throwable e) {
			LinkDataHookManager.endLinkBatch(false);
			rollback(monitor);
			EclipseLog.error("Error during data link", e, Activator.getDefault());
			return generateError(e);
		}

		return Status.OK_STATUS;
	}
	
	private void rollback(IProgressMonitor monitor) {
		// Something to undo / delete?
	}
	
	private IStatus generateError(Throwable e) {
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e.getCause());
	}
	
	private Plate doLinkData(PlateReading reading, int sequence, IProgressMonitor monitor) throws DataLinkException {
		
		// Locate the HDF-5 file and image file
		SecureFileServer fs = Screening.getEnvironment().getFileServer();
		HDF5File hdf5File = new HDF5File(fs.getBasePath() + reading.getCapturePath(), true);
		
		Plate plate = null;
		if (task.createNewPlates) {
			plate = prepareNewPlate(reading, sequence);
		} else {
			plate = task.mappedReadings.get(reading);
		}

		// The components that will be executed, in this order.
		List<IDataLinkerComponent> components = new ArrayList<IDataLinkerComponent>();
		if (task.linkPlateData) components.add(new PlateDataLinker());
		if (task.linkWellData) components.add(new WellDataLinker());
		if (task.linkSubWellData) components.add(new SubWellDataLinker());
		if (task.linkImageData) components.add(new ImageDataLinker());
		
		try {
			for (IDataLinkerComponent comp: components) {
				comp.prepareLink(reading, hdf5File, plate, monitor);
			}
			// All preparations succeeded. Proceed to persist.
			for (IDataLinkerComponent comp: components) {
				comp.executeLink();
			}
			reading.setLinkStatus(1);
		} catch (DataLinkException e) {
			for (IDataLinkerComponent comp: components) {
				comp.rollback();
			}
			reading.setLinkStatus(-1);
			throw e;
		} finally {
			hdf5File.close();
			
			reading.setLinkDate(new Date());
			reading.setLinkUser(SecurityService.getInstance().getCurrentUserName());
			DataCaptureService.getInstance().updateReading(reading);
		}
		
		return plate;
	}
	
	private Plate prepareNewPlate(PlateReading reading, int sequence) {
		int rows = reading.getRows();
		rows = rows == 0 ? 16 : rows;
		int cols = reading.getColumns();
		cols = cols == 0 ? 24 : cols;
		
		if (reading.getFileInfo() != null && NumberUtils.isNumeric(reading.getFileInfo())) {
			sequence = Integer.parseInt(reading.getFileInfo());
		}
		
		Plate plate = PlateService.getInstance().createPlate(task.targetExperiment, rows, cols);
		plate.setSequence(sequence);
		plate.setBarcode(reading.getBarcode());
		
		return plate;
	}
}
