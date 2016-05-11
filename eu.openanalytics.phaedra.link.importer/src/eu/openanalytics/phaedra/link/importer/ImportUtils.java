package eu.openanalytics.phaedra.link.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.config.CaptureConfig;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class ImportUtils {

	public final static String PLATE_LOCATOR_ID = "locate.plates";
	public final static String WELLDATA_IMPORTER_ID = "gather.welldata";
	public final static String SUBWELLDATA_IMPORTER_ID = "gather.subwelldata";
	public final static String IMAGEDATA_IMPORTER_ID = "gather.imagedata";
	
	public static String getCaptureConfigId(Protocol protocol) {
		return DataCaptureService.getInstance().getCaptureConfigId(protocol.getId());
	}

	public static String[] createFilter(String captureConfigId, boolean well, boolean subWell, boolean image) {
		try {
			CaptureConfig captureConfig = DataCaptureService.getInstance().getCaptureConfig(captureConfigId);
			
			String[] wellModules 	= getModules(WELLDATA_IMPORTER_ID, captureConfig);
			String[] subwellModules = getModules(SUBWELLDATA_IMPORTER_ID, captureConfig);
			String[] imageModules 	= getModules(IMAGEDATA_IMPORTER_ID, captureConfig);
			
			List<String> filter = new ArrayList<String>();
			ModuleConfig[] configs = captureConfig.getModuleConfigs();
			for (ModuleConfig cfg: configs) {
				String id = cfg.getId();

				// Skip module only if explicitly disabled. I.e. by default, execute everything.
				if (!well && CollectionUtils.find(wellModules, id) != -1) continue;
				if (!subWell && CollectionUtils.find(subwellModules, id) != -1) continue;
				if (!image && CollectionUtils.find(imageModules, id) != -1) continue;
				
				filter.add(id);
			}

			return filter.toArray(new String[filter.size()]);
		} 
		catch (IOException e) 
		{
			return null;
		}
		catch(DataCaptureException e)
		{
			throw new RuntimeException(e.getMessage(), e.getCause());
		}
	}

	private static String[] getModules(String baseId, CaptureConfig cfg) {
		if (baseId == null || baseId.isEmpty()) return new String[0];
		baseId = baseId.trim().toLowerCase();
		List<String> matchingIds = new ArrayList<String>();
		for (ModuleConfig mod: cfg.getModuleConfigs()) {
			String id = mod.getId();
			if (id.toLowerCase().startsWith(baseId)) matchingIds.add(id);
		}
		return matchingIds.toArray(new String[matchingIds.size()]);
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
			task.setTest(true); // Do not save the plate readings!

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
