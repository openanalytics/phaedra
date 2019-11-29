package eu.openanalytics.phaedra.datacapture.scripting;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.imaging.util.ImageIdentifier;
import eu.openanalytics.phaedra.base.imaging.util.TIFFCodec;
import eu.openanalytics.phaedra.base.scripting.api.IScriptAPIProvider;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.parser.ParserService;
import eu.openanalytics.phaedra.datacapture.parser.util.ModelUtils;
import eu.openanalytics.phaedra.datacapture.parser.util.ParserUtils;
import eu.openanalytics.phaedra.datacapture.util.CaptureUtils;

public class ScriptAPI implements IScriptAPIProvider {

	@Override
	public Map<String, Object> getServices() {
		Map<String, Object> utils = new HashMap<>();
		utils.put("DataCaptureService", DataCaptureService.getInstance());
		utils.put("ParserService", ParserService.getInstance());
		utils.put("CaptureUtils", CaptureUtils.class);
		utils.put("ModelUtils", ModelUtils.class);
		utils.put("ParserUtils", ParserUtils.class);
		utils.put("ImageIdentifier", ImageIdentifier.class);
		utils.put("TIFFCodec", TIFFCodec.class);
		return utils;
	}

	@Override
	public String getHelp(String service) {
		return null;
	}

}
