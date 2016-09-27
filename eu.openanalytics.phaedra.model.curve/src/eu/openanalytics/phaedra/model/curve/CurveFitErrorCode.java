package eu.openanalytics.phaedra.model.curve;

import java.util.Arrays;

import eu.openanalytics.phaedra.model.curve.vo.Curve;

public class CurveFitErrorCode {

	public int code;
	public String description;
	
	public CurveFitErrorCode(int code, String description) {
		this.code = code;
		this.description = description;
	}

	public static String getDescription(Curve curve) {
		ICurveFitModel model = CurveFitService.getInstance().getModel(curve.getModelId());
		if (model == null) return null;
		if (curve.getErrorCode() == 0) return "No error. The fit succeeded normally.";
		return Arrays.stream(model.getErrorCodes()).filter(e -> e.code == curve.getErrorCode()).map(e -> e.description).findAny().orElse(null);
	}
}
