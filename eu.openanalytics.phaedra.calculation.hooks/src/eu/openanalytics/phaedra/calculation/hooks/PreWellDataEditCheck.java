package eu.openanalytics.phaedra.calculation.hooks;

import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.hook.WellDataActionHookArguments;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;

public class PreWellDataEditCheck implements IHook {

	@Override
	public void pre(IHookArguments hookArgs) throws PreHookException {
		WellDataActionHookArguments args = (WellDataActionHookArguments) hookArgs;
		if (args.action != ModelEventType.ObjectChanged) return;
		if (!args.feature.isClassificationRestricted()) return;
		
		for (Well well: args.plate.getWells()) {
			int wellNr = PlateUtils.getWellNr(well);
		
			// Note: NaN and null are allowed, as they represent the 'unclassified' status.
			boolean matches = false;
			String stringValue = null;
			if (args.feature.isNumeric()) {
				double value = ((double[])args.values)[wellNr-1];
				stringValue = Formatters.getInstance().format(value, args.feature);
				matches = (Double.isNaN(value) || ClassificationService.getInstance().getHighestClass(value, args.feature) != null);
			} else {
				stringValue = ((String[])args.values)[wellNr-1];
				matches = (stringValue == null || ClassificationService.getInstance().getHighestClass(stringValue, args.feature) != null);
			}
			
			if (!matches) {
				throw new PreHookException("Invalid value for well " + PlateUtils.getWellCoordinate(well) + ": feature '" + args.feature 
						+ "' has restricted classification and value '" + stringValue + "' does not match any class");
			}
		}
	}

	@Override
	public void post(IHookArguments args) {
		// Do nothing.
	}

}
