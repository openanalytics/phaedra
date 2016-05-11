package eu.openanalytics.phaedra.model.plate;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;

public class ScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("PlateService", PlateService.getInstance());
		utils.put("PlateUtils", PlateUtils.class);
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

}
