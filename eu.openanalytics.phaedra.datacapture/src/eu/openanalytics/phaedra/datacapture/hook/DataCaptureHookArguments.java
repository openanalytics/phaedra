package eu.openanalytics.phaedra.datacapture.hook;

import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.datacapture.DataCaptureContext;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class DataCaptureHookArguments implements IHookArguments {
	
	public DataCaptureContext context;
	public PlateReading reading;
	public Plate plate;

}
