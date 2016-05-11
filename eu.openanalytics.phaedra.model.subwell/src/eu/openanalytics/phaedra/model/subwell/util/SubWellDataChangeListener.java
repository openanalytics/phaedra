package eu.openanalytics.phaedra.model.subwell.util;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellDataChangeListener implements IModelEventListener {
	
	@Override
	public void handleEvent(ModelEvent event) {
		if (event.type == ModelEventType.ObjectChanged) {
			List<Well> affectedWells = new ArrayList<>();
			Object[] objects = ModelEventService.getEventItems(event);
			for (Object o: objects) {
				if (o instanceof Well) affectedWells.add((Well)o);
			}
			handle(affectedWells);
		}
	}

	protected void handle(List<Well> affectedWells) {
		// Default: do nothing.
	}
}
