package eu.openanalytics.phaedra.datacapture.store.persist;

import java.io.File;
import java.io.IOException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationTrigger;
import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.store.HDF5FileStore;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;

public class SubWellHDF5DataPersistor extends BaseDataPersistor {

	@Override
	public void persist(IFileStore store, Plate plate) throws DataCaptureException, IOException {
		EclipseLog.info(String.format("Thread %s running %s", Thread.currentThread().getName(), this.getClass().getName()), Activator.PLUGIN_ID);
		
		HDF5File file = ((HDF5FileStore) store).getFile();
		if (!file.exists(HDF5File.getSubWellDataPath())) return;
		
		String destination = PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
		try {
			Screening.getEnvironment().getFileServer().putContents(destination, new File(file.getFilePath()));
		} catch (IOException e) {
			throw new DataCaptureException("File upload failed at " + destination, e);
		}

		plate.setSubWellDataAvailable(true);
		PlateService.getInstance().updatePlate(plate);
		
		// If needed, trigger a subwell data calculation.
		boolean triggerSubwellCalculation = ProtocolUtils.getFeatures(plate).stream()
				.anyMatch(f -> f.isCalculated() && CalculationTrigger.SubwellDataChange.toString().equals(f.getCalculationTrigger()));
		if (triggerSubwellCalculation) CalculationService.getInstance().triggerSubWellCalculation(plate);
	}

}