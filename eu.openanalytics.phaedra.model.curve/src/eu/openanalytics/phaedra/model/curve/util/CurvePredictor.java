package eu.openanalytics.phaedra.model.curve.util;

import java.util.Arrays;
import java.util.stream.IntStream;

import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.fit.CurveDataPoints;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.OSBCurve;
import eu.openanalytics.phaedra.model.curve.vo.PLACCurve;

/**
 * "Predict" a number of points of a dose-response curve by inspecting the fitted curve's model, bounds and hill.
 */
public class CurvePredictor {

	public static double[][] predict(Curve curve, int size) {
		if (curve instanceof OSBCurve) {
			return predictOSB((OSBCurve)curve, size);
		} else if (curve instanceof PLACCurve) {
			return predictPLAC((PLACCurve)curve, size);
		}
		return null;
	}
	
	private static double[][] predictPLAC(PLACCurve curve, int size) {
		
		if ((curve.getPlacCensor() != null && !curve.getPlacCensor().isEmpty()) || Double.isNaN(curve.getPlac())) {
			return new double[2][0];
		}
		
		CurveDataPoints fitData = CurveService.getInstance().getDataPoints(curve);
		
		size = 0;
		for (int i = 0; i < fitData.concs.length; i++) {
			if (fitData.accepts[i] > 0)
				size++;
		}
		double[][] v = new double[2][size];

		size = 0;
		for (int i = 0; i < fitData.concs.length; i++) {
			if (fitData.accepts[i] > 0) {
				v[0][size] = -fitData.concs[i];
				v[1][size] = fitData.values[i];
				size++;
			}
		}
		return v;
	}
	
	private static double[][] predictOSB(OSBCurve curve, int size) {
		
		if ("<".equals(curve.getPic50Censor()) 
				|| ">".equals(curve.getPic50Censor()) 
				|| (Double.isNaN(curve.getPic50()))) {
			return new double[2][0];
		}

		String method = curve.getSettings().getMethod();
		if (method.equals("OLS"))
			return getOLSPredictedValues(curve, size);
		if (method.equals("LIN"))
			return getLINPredictedValues(curve);

		return new double[2][0];
	}
	
	private static double[][] getOLSPredictedValues(OSBCurve curve, int size) {
		double[][] v = new double[2][size];

		CurveDataPoints fitData = CurveService.getInstance().getDataPoints(curve);
		
		// Make subset to remove rejected points.
		int acceptedPts = 0;
		for (int i=0;i<fitData.accepts.length;i++) {
			if (fitData.accepts[i] >= 0) acceptedPts++;
		}
		double[] acceptedConcs = new double[acceptedPts];
		int index = 0;
		for (int i=0;i<fitData.concs.length;i++) {
			if (fitData.accepts[i] >= 0) acceptedConcs[index++] = fitData.concs[i];
		}
		
		double minX = StatService.getInstance().calculate("min", acceptedConcs);
		double maxX = StatService.getInstance().calculate("max", acceptedConcs);
		double stepX = (maxX - minX) / size;
		double conc = minX;

		double hillUsed = curve.getHill();
		if ("D".equals(curve.getSettings().getType())) hillUsed = -hillUsed;

		double lbUsed = curve.getLb();
		if (Double.isNaN(lbUsed)) lbUsed = Math.min(fitData.lcMedian, fitData.hcMedian);
		double ubUsed = curve.getUb();
		if (Double.isNaN(ubUsed)) ubUsed = Math.max(Double.isNaN(fitData.lcMedian) ? 0.0 : fitData.lcMedian, fitData.hcMedian);
		
		String model = curve.getSettings().getModel();
		if (model.startsWith("PL4")) {
			lbUsed = curve.getLb();
			ubUsed = curve.getUb();
		} else if (model.startsWith("PL3L")) {
			ubUsed = curve.getUb();
		} else if (model.startsWith("PL3U")) {
			lbUsed = curve.getLb();
		}
		
		// Fix for curves that have no control wells: assume LB (or UB) is 0 by default.
		if (Double.isNaN(lbUsed)) lbUsed = 0;
		if (Double.isNaN(ubUsed)) ubUsed = 0;
		
		for (int i = 0; i < size; i++) {
//			double noemer = 1 + Math.exp(-hillUsed * (curve.getPic50() - conc) * Math.E);
			double noemer = 1 + Math.pow(10, hillUsed*(conc - curve.getPic50()));
			double breuk = (ubUsed - lbUsed) / noemer;
			v[0][i] = -conc;
			v[1][i] = lbUsed + breuk;
			conc += stepX;
		}

		return v;
	}

	private static double[][] getLINPredictedValues(OSBCurve curve) {
		// zoek de twee points rondom de pIC50
		double[] lineStart = new double[] { Double.NaN, Double.NaN };
		double[] lineEnd = new double[] { Double.NaN, Double.NaN };
		
		CurveDataPoints fitData = CurveService.getInstance().getDataPoints(curve);
		int[] validPoints = IntStream.range(0, fitData.concs.length).filter(i -> fitData.accepts[i] >= 0).toArray();

		double concBefore = Arrays.stream(validPoints).mapToDouble(i -> fitData.concs[i]).filter(d -> d < curve.getPic50()).max().orElse(Double.NaN);
		double concAfter = Arrays.stream(validPoints).mapToDouble(i -> fitData.concs[i]).filter(d -> d > curve.getPic50()).min().orElse(Double.NaN);
		
		if (!Double.isNaN(concBefore) && !Double.isNaN(concAfter)) {
			lineStart[0] = -concAfter;
			lineStart[1] = Arrays.stream(validPoints)
				.filter(i -> fitData.concs[i] == -lineStart[0])
				.mapToDouble(i -> fitData.values[i])
				.average().orElse(Double.NaN);
			
			lineEnd[0] = -concBefore;
			lineEnd[1] = Arrays.stream(validPoints)
				.filter(i -> fitData.concs[i] == -lineEnd[0])
				.mapToDouble(i -> fitData.values[i])
				.average().orElse(Double.NaN);
		}
		
		return new double[][] { { lineStart[0], lineEnd[0] } , { lineStart[1], lineEnd[1] } };
	}
}
