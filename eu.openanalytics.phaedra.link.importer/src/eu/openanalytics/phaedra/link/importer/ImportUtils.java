package eu.openanalytics.phaedra.link.importer;

import java.util.List;

import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
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
}
