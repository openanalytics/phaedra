package eu.openanalytics.phaedra.model.curve.render;

import eu.openanalytics.phaedra.model.curve.CurveFitInput;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public class BaseCurveRenderer implements ICurveRenderer {

	@Override
	public String[] getSupportedModelIds() {
		// Support nothing by default.
		return null;
	}
	
	@Override
	public double[][] getCurveSamples(Curve curve, CurveFitInput input) {
		// Curve samples cannot be calculated.
		return null;
	}

	@Override
	public CurveRendererType getCurveRendererType(Curve curve, CurveFitInput input) {
		return CurveRendererType.Line;
	}
	
	@Override
	public double[] getPointWeights(Curve curve, CurveFitInput input) {
		// Point weights are not known.
		return null;
	}

	@Override
	public double[] getPlotRange(Curve curve, CurveFitInput input) {
		// Plot range is unknown.
		return null;
	}

	@Override
	public CurveAnnotation[] getAnnotations(Curve curve, CurveFitInput input) {
		return null;
	}

	@Override
	public CurveBand[] getBands(Curve curve, CurveFitInput input) {
		return null;
	}

	@Override
	public CurveDomainInterval[] getDomainIntervals(Curve curve, CurveFitInput input) {
		return null;
	}

}
