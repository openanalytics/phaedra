package eu.openanalytics.phaedra.link.data.hook;

import java.util.List;

import eu.openanalytics.phaedra.base.hook.IBatchedHookArguments;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.link.data.DataLinkTask;

public class LinkDataBatchedHookArguments implements IBatchedHookArguments {

	public DataLinkTask task;
	public List<PlateReading> readings;
	
	public LinkDataBatchedHookArguments(DataLinkTask task, List<PlateReading> readings) {
		this.task = task;
		this.readings = readings;
	}
}
