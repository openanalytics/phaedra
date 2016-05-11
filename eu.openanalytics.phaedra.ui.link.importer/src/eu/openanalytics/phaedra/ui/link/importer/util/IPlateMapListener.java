package eu.openanalytics.phaedra.ui.link.importer.util;

import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.vo.Plate;


public interface IPlateMapListener {

	public void plateMapped(PlateReading source, Plate plate);

}
