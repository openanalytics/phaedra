package eu.openanalytics.phaedra.link.hooks;

import java.util.HashSet;
import java.util.Set;

import eu.openanalytics.phaedra.base.hook.BaseBatchedHook;
import eu.openanalytics.phaedra.base.hook.IHookArguments;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.hooks.CalculationHookUtils;
import eu.openanalytics.phaedra.link.data.hook.LinkDataHookArguments;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class PostDataLinkCalculator extends BaseBatchedHook {

	private Set<Plate> platesToRecalc = new HashSet<>();
	
	@Override
	protected void processPost(IHookArguments args) {
		Plate plate = ((LinkDataHookArguments)args).plate;
		platesToRecalc.addAll(CalculationHookUtils.getNormalizationLinkedPlates(plate));
	}
	
	@Override
	protected void processBatch() {
		for (Plate plate: platesToRecalc) CalculationService.getInstance().calculate(plate);
	}
	
	@Override
	protected void reset() {
		platesToRecalc.clear();
	}
}
