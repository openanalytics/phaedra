package eu.openanalytics.phaedra.model.plate.hook;

import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PlateActionHookArguments implements IHookArguments {

	public Plate plate;
	public ModelEventType action;
	
	public PlateActionHookArguments(Plate plate, ModelEventType action) {
		this.plate = plate;
		this.action = action;
	}
}
