package eu.openanalytics.phaedra.model.curve.util;

import java.util.ArrayList;

import eu.openanalytics.phaedra.model.curve.CurveService.CurveKind;
import eu.openanalytics.phaedra.model.curve.fit.ICurveFitter;
import eu.openanalytics.phaedra.model.curve.fit.OSBCurveFitter;
import eu.openanalytics.phaedra.model.curve.fit.PLACCurveFitter;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.CurveSettings;
import eu.openanalytics.phaedra.model.curve.vo.OSBCurve;
import eu.openanalytics.phaedra.model.curve.vo.PLACCurve;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CurveFactory {

	public static Curve newCurve(Feature f) {
		if (f == null || f.getCurveSettings() == null) return null;
		String kindString = f.getCurveSettings().get(CurveSettings.KIND);
		if (kindString == null || kindString.isEmpty()) return null;
		Curve curve = newCurve(kindString);
		if (curve != null) curve.setFeature(f);
		return curve;
	}
	
	public static Curve newCurve(String kindString) {
		CurveKind kind = CurveKind.valueOf(kindString);
		Curve curve = null;
		switch (kind) {
		case OSB: curve = new OSBCurve(); break;
		case PLAC: curve = new PLACCurve(); break;
		}
		if (curve != null) curve.setCompounds(new ArrayList<>());
		return curve;
	}

	public static ICurveFitter getFitter(Feature f) {
		if (f == null || f.getCurveSettings() == null) return null;
		String kindString = f.getCurveSettings().get(CurveSettings.KIND);
		if (kindString == null || kindString.isEmpty()) return null;
		
		CurveKind kind = CurveKind.valueOf(kindString);
		switch (kind) {
		case OSB: return new OSBCurveFitter();
		case PLAC: return new PLACCurveFitter();
		}
		return null;
	}
	
	public static int getDefaultThreshold(Feature f) {
		if (f == null || f.getCurveSettings().isEmpty()) return 0;
		String kind = f.getCurveSettings().get(CurveSettings.KIND);
		if (CurveKind.PLAC.name().equals(kind)) {
			return 3;
		}
		return 0;
	}
}
