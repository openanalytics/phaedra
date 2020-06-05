package eu.openanalytics.phaedra.link.importer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class ImportUtils {

	public final static String PLATE_LOCATOR_ID = "locate.plates";
	
	public static String getCaptureConfigId(Protocol protocol) {
		return DataCaptureService.getInstance().getCaptureConfigId(protocol.getId());
	}

	public static String[] createFilter(String captureConfigId, boolean well, boolean subWell, boolean image) {
		return CaptureUtils.createModuleFilter(captureConfigId, well, subWell, image);
	}

	public static PlateReading[] locatePlates(String path, String captureConfigId) {
		PlateReading[] readings = new PlateReading[0];
		try {
			// Find the plate locator module, if any.
			CaptureConfig captureConfig = DataCaptureService.getInstance().getCaptureConfig(captureConfigId);
			
			boolean found = false;
			for (ModuleConfig cfg: captureConfig.getModuleConfigs()) {
				if (cfg.getId().equals(PLATE_LOCATOR_ID)) {
					found = true;
					break;
				}
			}
			if (!found) {
				throw new RuntimeException("Cannot auto-locate plates: capture configuration"
						+ "'" + captureConfigId + "' has no '" + PLATE_LOCATOR_ID + "' module.");
			}

			DataCaptureTask task = DataCaptureService.getInstance().createTask(path, captureConfigId);
			task.setModuleFilter(new String[]{PLATE_LOCATOR_ID});
			task.setTest(true); // Do not save this capture!

			// Locate the plates.
			List<PlateReading> readingList = DataCaptureService.getInstance().executeTask(task, null);
			return readingList.toArray(readings);
		} catch (Exception e) {
			// Location failed: no readings available.
		}
		return readings;
	}
	
	public static PlateReading[] locatePlates(String path, Experiment exp) {
		PlateReading[] readings = new PlateReading[0];
		try
		{
			String captureConfigId = getCaptureConfigId(exp.getProtocol());
			readings = locatePlates(path, captureConfigId);
		} 
		catch (Exception e) 
		{
			// Location failed: no readings available.
		}
		return readings;
	}
	
	
	public static IStatus createExperiment(final ImportTask task,
			final Protocol protocol, final String name) {
		try {
			Experiment experiment = PlateService.getInstance().createExperiment(protocol);
			experiment.setName(name);
			PlateService.getInstance().updateExperiment(experiment);
			task.targetExperiment = experiment;
			return Status.OK_STATUS;
		} catch (Exception t) {
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to create Experiment.\n\n" + t.getMessage());
		}
	}
	
	public static IStatus precheckTask(final ImportTask task) {
		Collection<Experiment> experiments = Collections.emptyList();
		if (task.targetExperiment != null) {
			experiments = Collections.singletonList(task.targetExperiment);
		}
		else if (task.plateMapping != null) {
			experiments = task.plateMapping.values().stream()
					.map(Plate::getExperiment)
					.collect(Collectors.toSet());
		}
		if (!experiments.isEmpty()) {
			final List<Experiment> closed = experiments.stream().filter(Experiment::isClosed)
					.collect(Collectors.toList());
			if (!closed.isEmpty()) {
				final Experiment experiment0 = closed.get(0);
				return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						(experiments.size() == 1) ?
								String.format("The target experiment '%1$s' (%2$s) is closed.",
										experiment0.getName(), experiment0.getId() ) :
								String.format("%3$s of the target experiments ('%1$s' (%2$s), ...) are closed.",
										experiment0.getName(), experiment0.getId(), closed.size() ));
			}
		}
		
		return Status.OK_STATUS;
	}
	
}
