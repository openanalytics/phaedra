package eu.openanalytics.phaedra.base.scripting.api;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;

public class BaseScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("ScriptService", ScriptService.getInstance());
		utils.put("StreamUtils", StreamUtils.class);
		utils.put("FileUtils", FileUtils.class);
		utils.put("NumberUtils", NumberUtils.class);
		utils.put("ProcessUtils", ProcessUtils.class);
		utils.put("StringUtils", StringUtils.class);
		utils.put("XmlUtils", XmlUtils.class);
		utils.put("ImageUtils", ImageUtils.class);
		utils.put("ColorUtils", ColorUtils.class);
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

}
