package eu.openanalytics.phaedra.model.protocol;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;

public class ScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("ProtocolService", ProtocolService.getInstance());
		utils.put("ProtocolUtils", ProtocolUtils.class);
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

}
