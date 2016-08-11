package eu.openanalytics.phaedra.base.hdf5.util;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;

public class ScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("HDF5Utils", HDF5Utils.class);
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

	public static class HDF5Utils {
		public static HDF5File open(String path, boolean readonly) {
			return new HDF5File(path, readonly);
		}
	}
}
