package eu.openanalytics.phaedra.model.curve.fit;

import eu.openanalytics.phaedra.model.curve.CurveService.CurveKind;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public interface ICurveFitter {

	public CurveKind getSupportedKind();
	
	public void fit(Curve curve) throws CurveFitException;
}
