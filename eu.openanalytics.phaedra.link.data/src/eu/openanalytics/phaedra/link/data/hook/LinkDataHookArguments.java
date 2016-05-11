package eu.openanalytics.phaedra.link.data.hook;

import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.DataLinkTask;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class LinkDataHookArguments implements IHookArguments {

	public DataLinkTask task;
	public PlateReading reading;
	public Plate plate;
	
	public LinkDataHookArguments(DataLinkTask task, PlateReading reading, Plate plate) {
		this.task = task;
		this.reading = reading;
		this.plate = plate;
	}
}
