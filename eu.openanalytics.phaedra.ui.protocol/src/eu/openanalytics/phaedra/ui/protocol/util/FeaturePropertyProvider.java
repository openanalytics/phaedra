package eu.openanalytics.phaedra.ui.protocol.util;

import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveFitSettings;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class FeaturePropertyProvider {

	public static String[] getKeys() {
		return new String[] {"Name", "Id", "Protocol Class", "Key", "Numeric", "Curve"};
	}
	
	public static String getValue(String key, Feature f) {
		switch(key) {
		case "Name": return f.getName();
		case "Id": return "" + f.getId();
		case "Protocol Class": return f.getProtocolClass().getName();
		case "Key": return f.isKey() ? "Yes":"No";
		case "Numeric": return f.isNumeric() ? "Yes":"No";
		case "Curve": {
			CurveFitSettings settings = CurveFitService.getInstance().getSettings(f);
			if (settings == null) return "None";
			return settings.getModelId();
		}
		default: return "";
		}
	}
}
