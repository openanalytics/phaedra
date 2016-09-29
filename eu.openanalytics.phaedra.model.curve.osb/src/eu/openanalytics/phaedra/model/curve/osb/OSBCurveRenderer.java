package eu.openanalytics.phaedra.model.curve.osb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.RGB;

import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.curve.CurveFitInput;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.osb.prefs.Prefs;
import eu.openanalytics.phaedra.model.curve.render.ICurveRenderer;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public class OSBCurveRenderer implements ICurveRenderer {

	@Override
	public String[] getSupportedModelIds() {
		return OSBFitModelFactory.MODEL_IDS;
	}
	
	@Override
	public double[][] getCurveSamples(Curve curve, CurveFitInput input) {
		double pIC50 = CurveParameter.find(curve.getOutputParameters(), "pIC50").numericValue;
		String censor = CurveParameter.find(curve.getOutputParameters(), "pIC50 Censor").stringValue;
		if ("<".equals(censor) || ">".equals(censor) || (Double.isNaN(pIC50))) {
			return new double[2][0];
		}
		Value method = CurveParameter.find(curve.getOutputParameters(), "Method Fallback");
		if (method == null || method.stringValue == null) method = CurveParameter.find(input.getSettings().getExtraParameters(), "Method");
		if (method.stringValue.equals("OLS")) return getOLSPredictedValues(curve, input);
		else if (method.stringValue.equals("LIN")) return getLINPredictedValues(curve, input);
		else return new double[2][0];
	}
	
	@Override
	public double[] getPointWeights(Curve curve, CurveFitInput input) {
		Value weightValue = CurveParameter.find(curve.getOutputParameters(), "Weights");
		if (weightValue == null) return null;
		else return (double[]) CurveParameter.getBinaryValue(weightValue);
	}

	@Override
	public double[] getPlotRange(Curve curve, CurveFitInput input) {
		double[] bounds = {
				CurveParameter.find(curve.getOutputParameters(), "pIC50 LB").numericValue,
				CurveParameter.find(curve.getOutputParameters(), "pIC50 UB").numericValue
		};
		for (int i = 0; i < input.getValid().length; i++) {
			if (input.getValid()[i]) {
				bounds[0] = Math.min(bounds[0], input.getValues()[i]);
				bounds[1] = Math.max(bounds[1], input.getValues()[i]);
			}
		}
		
		double diff = bounds[1] - bounds[0];
		if (diff > 1.0d) return bounds;
		return new double[] {
				bounds[0] - diff/10,
				bounds[1] + diff/10
		};
	}
	
	@Override
	public CurveAnnotation[] getAnnotations(Curve curve, CurveFitInput input) {
		List<CurveAnnotation> annotations = new ArrayList<>();
		
		double[] bounds = {
				CurveParameter.find(curve.getOutputParameters(), "pIC50 LB").numericValue,
				CurveParameter.find(curve.getOutputParameters(), "pIC50 UB").numericValue
		};
		double[] ic50 = new double[] { 
			-CurveParameter.find(curve.getOutputParameters(), "pIC50").numericValue,
			(bounds[0] + (bounds[1] - bounds[0]) / 2)	
		};
		
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		boolean showIC50 = prefs.getBoolean(Prefs.CRC_SHOW_PIC50_MARKER);
		boolean showICx = prefs.getBoolean(Prefs.CRC_SHOW_OTHER_IC_MARKERS);
		int boundThickness = prefs.getInt(Prefs.CRC_BOUND_THICKNESS);
		RGB lbColor = ColorUtils.parseRGBString(prefs.getString(Prefs.CRC_BOUND_COLOR_LOWER));
		RGB ubColor = ColorUtils.parseRGBString(prefs.getString(Prefs.CRC_BOUND_COLOR_UPPER));
		
		String censor = CurveParameter.find(curve.getOutputParameters(), "pIC50 Censor").stringValue;
		boolean ic50Censored = ("<".equals(censor) || ">".equals(censor));
		if (!ic50Censored) {
			if (showIC50) {
				annotations.add(new CurveAnnotation(2.0f, 0x000000, ic50[0], Integer.MIN_VALUE, ic50[0], ic50[1]));
				annotations.add(new CurveAnnotation(2.0f, 0x000000, Integer.MIN_VALUE, ic50[1], ic50[0], ic50[1]));
			}
			if (showICx) {
				double[] ic20 = new double[] { -CurveParameter.find(curve.getOutputParameters(), "pIC20").numericValue, (bounds[0] + (bounds[1] - bounds[0]) * 0.2) };
				annotations.add(new CurveAnnotation(2.0f, 0x000000, ic20[0], Integer.MIN_VALUE, ic20[0], ic20[1]));
				annotations.add(new CurveAnnotation(2.0f, 0x000000, Integer.MIN_VALUE, ic50[1], ic50[0], ic50[1]));
				double[] ic80 = new double[] { -CurveParameter.find(curve.getOutputParameters(), "pIC80").numericValue, (bounds[0] + (bounds[1] - bounds[0]) * 0.8) };
				annotations.add(new CurveAnnotation(2.0f, 0x000000, ic80[0], Integer.MIN_VALUE, ic80[0], ic80[1]));
				annotations.add(new CurveAnnotation(2.0f, 0x000000, Integer.MIN_VALUE, ic80[1], ic80[0], ic80[1]));
			}
		}
		annotations.add(new CurveAnnotation(boundThickness, ColorUtils.rgbToHex(lbColor), Integer.MIN_VALUE, bounds[0], Integer.MAX_VALUE, bounds[0]));
		annotations.add(new CurveAnnotation(boundThickness, ColorUtils.rgbToHex(ubColor), Integer.MIN_VALUE, bounds[1], Integer.MAX_VALUE, bounds[1]));
		return annotations.toArray(new CurveAnnotation[annotations.size()]);
	}

	@Override
	public CurveBand[] getBands(Curve curve, CurveFitInput input) {
		IPreferenceStore prefs = Activator.getDefault().getPreferenceStore();
		if (!prefs.getBoolean(Prefs.CRC_SHOW_CONF_AREA)) return new CurveBand[0];
		
		double[][] ciGrid = (double[][]) CurveParameter.getBinaryValue(CurveParameter.find(curve.getOutputParameters(), "Confidence Band"));
		if (ciGrid == null) return null;
		
		int points = ciGrid[0].length;
		double[] lowerX = new double[points];
		double[] lowerY = new double[points];
		double[] upperX = new double[points];
		double[] upperY = new double[points];
		
		for (int i=0; i<points; i++) {
			lowerX[i] = ciGrid[0][i]/Math.log(10);
			lowerY[i] = ciGrid[1][i];
			upperX[i] = ciGrid[0][i]/Math.log(10);
			upperY[i] = ciGrid[2][i];
		}
		
		float alpha = prefs.getFloat(Prefs.CRC_CONF_AREA_ALPHA) / 100.0f;
		RGB bandColor = ColorUtils.parseRGBString(prefs.getString(Prefs.CRC_CONF_AREA_COLOR));
		CurveBand ciBand = new CurveBand(ColorUtils.rgbToHex(bandColor), alpha, lowerX, lowerY, upperX, upperY);
		return new CurveBand[] { ciBand };
	}

	@Override
	public CurveDomainInterval[] getDomainIntervals(Curve curve, CurveFitInput input) {
		double[] ci = new double[] {
			CurveParameter.find(curve.getOutputParameters(), "pIC50 LCL").numericValue,
			CurveParameter.find(curve.getOutputParameters(), "pIC50 UCL").numericValue
		};
		CurveDomainInterval[] intervals = new CurveDomainInterval[1];
		intervals[0] = new CurveDomainInterval(0xFFFF77, 0.25f, ci[0], ci[1]);
		return intervals;
	}

	private static double[][] getOLSPredictedValues(Curve curve, CurveFitInput input) {
		int size = 30;
		double[][] v = new double[2][size];

		// Make subset to remove rejected points.
		int acceptedPts = 0;
		for (int i=0;i<input.getValid().length;i++) {
			if (input.getValid()[i]) acceptedPts++;
		}
		double[] acceptedConcs = new double[acceptedPts];
		int index = 0;
		for (int i=0;i<input.getConcs().length;i++) {
			if (input.getValid()[i]) acceptedConcs[index++] = input.getConcs()[i];
		}
		
		double minX = StatService.getInstance().calculate("min", acceptedConcs);
		double maxX = StatService.getInstance().calculate("max", acceptedConcs);
		double stepX = (maxX - minX) / size;
		double conc = minX;

		double hillUsed = CurveParameter.find(curve.getOutputParameters(), "Hill").numericValue;
		String type = CurveParameter.find(input.getSettings().getExtraParameters(), "Type").stringValue;
		if ("D".equals(type)) hillUsed = -hillUsed;

		double lbUsed = CurveParameter.find(curve.getOutputParameters(), "pIC50 LB").numericValue;
		double ubUsed = CurveParameter.find(curve.getOutputParameters(), "pIC50 UB").numericValue;
		//TODO Still needed?
//		if (Double.isNaN(lbUsed)) lbUsed = Math.min(fitData.lcMedian, fitData.hcMedian);
//		if (Double.isNaN(ubUsed)) ubUsed = Math.max(Double.isNaN(fitData.lcMedian) ? 0.0 : fitData.lcMedian, fitData.hcMedian);
//		String model = curve.getSettings().getModel();
//		if (model.startsWith("PL4")) {
//			lbUsed = curve.getLb();
//			ubUsed = curve.getUb();
//		} else if (model.startsWith("PL3L")) {
//			ubUsed = curve.getUb();
//		} else if (model.startsWith("PL3U")) {
//			lbUsed = curve.getLb();
//		}
		
		// Fix for curves that have no control wells: assume LB (or UB) is 0 by default.
		if (Double.isNaN(lbUsed)) lbUsed = 0;
		if (Double.isNaN(ubUsed)) ubUsed = 0;
		
		for (int i = 0; i < size; i++) {
			double pIC50 = CurveParameter.find(curve.getOutputParameters(), "pIC50").numericValue;
			double noemer = 1 + Math.pow(10, hillUsed*(conc - pIC50));
			double breuk = (ubUsed - lbUsed) / noemer;
			v[0][i] = -conc;
			v[1][i] = lbUsed + breuk;
			conc += stepX;
		}

		return v;
	}
	
	private static double[][] getLINPredictedValues(Curve curve, CurveFitInput input) {
		double[] lineStart = new double[] { Double.NaN, Double.NaN };
		double[] lineEnd = new double[] { Double.NaN, Double.NaN };
		
		int[] validPoints = IntStream.range(0, input.getConcs().length).filter(i -> input.getValid()[i]).toArray();
		double pIC50 = CurveParameter.find(curve.getOutputParameters(), "pIC50").numericValue;
		double concBefore = Arrays.stream(validPoints).mapToDouble(i -> input.getConcs()[i]).filter(d -> d < pIC50).max().orElse(Double.NaN);
		double concAfter = Arrays.stream(validPoints).mapToDouble(i -> input.getConcs()[i]).filter(d -> d > pIC50).min().orElse(Double.NaN);
		
		if (!Double.isNaN(concBefore) && !Double.isNaN(concAfter)) {
			lineStart[0] = -concAfter;
			lineStart[1] = Arrays.stream(validPoints)
				.filter(i -> input.getConcs()[i] == -lineStart[0])
				.mapToDouble(i -> input.getValues()[i])
				.average().orElse(Double.NaN);
			
			lineEnd[0] = -concBefore;
			lineEnd[1] = Arrays.stream(validPoints)
				.filter(i -> input.getConcs()[i] == -lineEnd[0])
				.mapToDouble(i -> input.getValues()[i])
				.average().orElse(Double.NaN);
		}
		
		return new double[][] { { lineStart[0], lineEnd[0] } , { lineStart[1], lineEnd[1] } };
	}
}
