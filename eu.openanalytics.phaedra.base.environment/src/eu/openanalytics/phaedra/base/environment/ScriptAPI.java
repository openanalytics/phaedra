package eu.openanalytics.phaedra.base.environment;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.environment.config.PasswordStore;
import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;

public class ScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("PasswordStore", new PasswordStore(null));
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

}
