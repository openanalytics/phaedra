package eu.openanalytics.phaedra.ui.protocol.util;

import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
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
			String kind = f.getCurveSettings().get(CurveSettings.KIND);
			if (kind == null || kind.isEmpty()) return "None";
			return kind 
					+ " (" + f.getCurveSettings().get(CurveSettings.MODEL) + ")"
					+ " (" + f.getCurveSettings().get(CurveSettings.METHOD) + ")"
					+ " (" + f.getCurveSettings().get(CurveSettings.TYPE) + ")";
		}
		default: return "";
		}
	}
}
