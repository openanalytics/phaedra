package eu.openanalytics.phaedra.model.subwell;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;
import eu.openanalytics.phaedra.model.subwell.util.SubWellUtils;

public class ScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("SubWellService", SubWellService.getInstance());
		utils.put("SubWellUtils", SubWellUtils.class);
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

}
