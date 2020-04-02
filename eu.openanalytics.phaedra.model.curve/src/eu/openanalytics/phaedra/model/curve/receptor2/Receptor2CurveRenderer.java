package eu.openanalytics.phaedra.model.curve.receptor2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.model.curve.CurveFitInput;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.render.ICurveRenderer;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public class Receptor2CurveRenderer implements ICurveRenderer {

	@Override
	public String[] getSupportedModelIds() {
		return new String[] { Receptor2FitModel.MODEL_ID };
	}

	@Override
	public double[][] getCurveSamples(Curve curve, CurveFitInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CurveRendererType getCurveRendererType(Curve curve, CurveFitInput input) {
		return CurveRendererType.Line;
	}

	@Override
	public double[] getPointWeights(Curve curve, CurveFitInput input) {
		return null;
	}

	@Override
	public double[] getPlotRange(Curve curve, CurveFitInput input) {
		double[] bounds = {
				CurveParameter.find(curve.getOutputParameters(), "Bottom").numericValue,
				CurveParameter.find(curve.getOutputParameters(), "Top").numericValue
		};
		for (int i = 0; i < input.getValid().length; i++) {
			if (input.getValid()[i] && !Double.isNaN(input.getValues()[i])) {
				bounds[0] = Math.min(bounds[0], input.getValues()[i]);
				bounds[1] = Math.max(bounds[1], input.getValues()[i]);
			}
		}
		double diff = bounds[1] - bounds[0];
		return new double[] {
				bounds[0] - diff/10,
				bounds[1] + diff/10
		};
	}

	@Override
	public CurveAnnotation[] getAnnotations(Curve curve, CurveFitInput input) {
		List<CurveAnnotation> annotations = new ArrayList<>();
		
		double[] bounds = {
				CurveParameter.find(curve.getOutputParameters(), "Bottom").numericValue,
				CurveParameter.find(curve.getOutputParameters(), "Top").numericValue
		};
		double[] ic50 = new double[] { 
			-CurveParameter.find(curve.getOutputParameters(), "pIC50").numericValue,
			(bounds[0] + (bounds[1] - bounds[0]) / 2)	
		};
		
		annotations.add(new CurveAnnotation(2.0f, 0x000000, ic50[0], Integer.MIN_VALUE, ic50[0], ic50[1]));
		annotations.add(new CurveAnnotation(2.0f, 0x000000, Integer.MIN_VALUE, ic50[1], ic50[0], ic50[1]));
		
		RGB lbColor = new RGB(0, 255, 0);;
		RGB ubColor = new RGB(255, 0, 0);
		
		annotations.add(new CurveAnnotation(3, ColorUtils.rgbToHex(lbColor), Integer.MIN_VALUE, bounds[0], Integer.MAX_VALUE, bounds[0]));
		annotations.add(new CurveAnnotation(3, ColorUtils.rgbToHex(ubColor), Integer.MIN_VALUE, bounds[1], Integer.MAX_VALUE, bounds[1]));
		
		return annotations.toArray(new CurveAnnotation[annotations.size()]);
	}

	@Override
	public CurveBand[] getBands(Curve curve, CurveFitInput input) {
		return new CurveBand[0];
	}

	@Override
	public CurveDomainInterval[] getDomainIntervals(Curve curve, CurveFitInput input) {
		return new CurveDomainInterval[0];
	}
	
	
}
