package eu.openanalytics.phaedra.model.curve;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;

public class ScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("CurveFitService", CurveFitService.getInstance());
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

}
