package eu.openanalytics.phaedra.datacapture.store.persist;

import java.io.IOException;

import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.CalculationService.CalculationTrigger;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;

public class SubWellDataPersistor extends BaseDataPersistor {

	@Override
	public void persist(IFileStore store, Plate plate) throws DataCaptureException, IOException {
		// TODO
//		plate.setSubWellDataAvailable(true);
//		PlateService.getInstance().updatePlate(plate);
		
		// If needed, trigger a subwell data calculation.
		boolean triggerSubwellCalculation = ProtocolUtils.getFeatures(plate).stream()
				.anyMatch(f -> f.isCalculated() && CalculationTrigger.SubwellDataChange.toString().equals(f.getCalculationTrigger()));
		if (triggerSubwellCalculation) CalculationService.getInstance().triggerSubWellCalculation(plate);
	}

}
