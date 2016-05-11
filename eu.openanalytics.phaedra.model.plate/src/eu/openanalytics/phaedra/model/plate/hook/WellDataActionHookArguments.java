package eu.openanalytics.phaedra.model.plate.hook;

import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class WellDataActionHookArguments implements IHookArguments {

	public Plate plate;
	public Feature feature;
	public Object values;
	public boolean normalized;
	public ModelEventType action;
	
	public WellDataActionHookArguments(Plate plate, Feature feature, Object values, boolean normalized, ModelEventType action) {
		this.plate = plate;
		this.feature = feature;
		this.values = values;
		this.normalized = normalized;
		this.action = action;
	}
}
