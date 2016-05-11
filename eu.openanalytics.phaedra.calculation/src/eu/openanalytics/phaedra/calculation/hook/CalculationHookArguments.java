package eu.openanalytics.phaedra.calculation.hook;

import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class CalculationHookArguments implements IHookArguments {

	public CalculationHookArguments(Plate plate) {
		this.plate = plate;
	}
	
	public Plate plate;
	
}
