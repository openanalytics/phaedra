package eu.openanalytics.phaedra.datacapture.store.persist;

import java.io.File;
import java.io.IOException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.store.DefaultDataCaptureStore;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class ImageDataPersistor implements IDataPersistor {

	@Override
	public void persist(IFileStore store, Plate plate) throws DataCaptureException, IOException {
		EclipseLog.info(String.format("Thread %s running %s", Thread.currentThread().getName(), this.getClass().getName()), Activator.PLUGIN_ID);
		
		String imagePath = store.readStringValue(DefaultDataCaptureStore.KEY_IMAGE_PATH);
		if (imagePath != null && new File(imagePath).isFile()) {
			String ext = FileUtils.getExtension(imagePath);
			String destination = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + "." + ext;
			Screening.getEnvironment().getFileServer().putContents(destination, new File(imagePath));
			FileUtils.deleteRecursive(imagePath);
			
			plate.setImageAvailable(true);
			PlateService.getInstance().updatePlate(plate);
		}
	}
}
