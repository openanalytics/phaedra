package eu.openanalytics.phaedra.calculation;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.hitcall.HitCallService;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.validation.ValidationService;

public class ScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("CalculationService", CalculationService.getInstance());
		utils.put("ValidationService", ValidationService.getInstance());
		utils.put("StatService", StatService.getInstance());
		utils.put("FormulaService", FormulaService.getInstance());
		utils.put("HitCallService", HitCallService.getInstance());
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

}
