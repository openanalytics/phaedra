package eu.openanalytics.phaedra.link.platedef.hook;

import eu.openanalytics.phaedra.base.hook.IHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.base.hook.PreHookException;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.compound.CompoundInfo;
import eu.openanalytics.phaedra.model.plate.compound.CompoundInfoService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PostLinkSaltformRetriever implements IHook {

	@Override
	public void pre(IHookArguments args) throws PreHookException {
		// Do nothing.
	}

	@Override
	public void post(IHookArguments args) {
		LinkPlateDefHookArguments linkArgs = (LinkPlateDefHookArguments)args;
		Plate plate = linkArgs.settings.getPlate();
		for (Compound compound: plate.getCompounds()) {
			CompoundInfo info = CompoundInfoService.getInstance().getInfo(compound);
			compound.setSaltform(info.getSaltform());
		}
		PlateService.getInstance().saveCompounds(plate);
	}

}