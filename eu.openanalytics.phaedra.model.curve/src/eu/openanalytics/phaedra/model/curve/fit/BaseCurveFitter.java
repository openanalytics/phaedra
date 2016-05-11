package eu.openanalytics.phaedra.model.curve.fit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import de.walware.rj.servi.RServi;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.utils.Graphic;
import eu.openanalytics.phaedra.base.r.rservi.CairoPdfGraphic;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveKind;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveType;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public abstract class BaseCurveFitter implements ICurveFitter {

	@Override
	public void fit(Curve curve) throws CurveFitException {
		
		CurveKind kind = CurveKind.valueOf(curve.getSettings().getKind());
		if (kind != getSupportedKind()) {
			throw new CurveFitException("Cannot use " + getClass().getSimpleName() + " on a " + kind.toString() + " curve");
		}
		
		CurveDataPoints fitData = CurveService.getInstance().getDataPoints(curve);
		doFitCurve(curve, fitData);
		calculateEmax(curve, fitData);
	}
	
	public abstract CurveKind getSupportedKind();
	
	protected abstract void doFitCurve(Curve curve, CurveDataPoints fitData) throws CurveFitException;

	protected byte[] getPlotImage(RServi rServi, Curve curve, String xLabel) throws CoreException, IOException {
		rServi.evalVoid("library(Cairo)", null);
		CairoPdfGraphic graphic = new CairoPdfGraphic();
		// Pdf inch size doesn't matter since we use vectorformat.
		graphic.setSize(4, 4, Graphic.UNIT_IN);
		
		FunctionCall plotFun = rServi.createFunctionCall("plot");
		plotFun.add("VALUE");
		plotFun.addChar("main", curve.getFeature().getDisplayName());
		if (xLabel != null) plotFun.addChar("xlab", xLabel);
		plotFun.addChar("ylab", curve.getFeature().getNormalization());

		byte[] plot = graphic.create(plotFun, rServi, null);
		return plot;
	}
	
	protected void calculateEmax(Curve curve, CurveDataPoints fitData) {
		double emaxConcentration = Double.NaN;
		double emaxEffect = Double.NaN;

		Map<Double, List<Double>> avgPointMap = new HashMap<Double, List<Double>>();
		int points = fitData.accepts.length;

		for (int point = 0; point < points; point++) {
			if ((fitData.accepts[point] >= 0) && (!Double.isNaN(fitData.values[point]))) {
				if (!avgPointMap.containsKey(fitData.concs[point])) {
					avgPointMap.put(fitData.concs[point], new ArrayList<Double>());
				}
				List<Double> l = avgPointMap.get(fitData.concs[point]);
				l.add(fitData.values[point]);
			}
		}

		// bereken averages per concentratie
		double avgConcs[] = new double[avgPointMap.size()];
		double avgValues[] = new double[avgPointMap.size()];
		int index = 0;
		for (double conc : avgPointMap.keySet()) {
			avgConcs[index] = conc;
			List<Double> l = avgPointMap.get(conc);
			double[] array = new double[l.size()];
			for (int a = 0; a < l.size(); a++)
				array[a] = l.get(a);
			double avg = StatService.getInstance().calculate("mean", array);
			avgValues[index] = avg;
			index++;
		}

		// find emax based on averages per concentration
		CurveType type = CurveType.valueOf(curve.getSettings().getType());
		if (type == CurveType.A) {
			for (int i = 0; i < avgConcs.length; i++) {
				if ((avgValues[i] > emaxEffect) || (Double.isNaN(emaxEffect))) {
					emaxEffect = avgValues[i];
					emaxConcentration = avgConcs[i];
				}
			}
		} else {
			for (int i = 0; i < avgConcs.length; i++) {
				if ((avgValues[i] < emaxEffect) || (Double.isNaN(emaxEffect))) {
					emaxEffect = avgValues[i];
					emaxConcentration = avgConcs[i];
				}
			}
		}
		
		curve.seteMax(emaxEffect);
		curve.seteMaxConc(emaxConcentration);
	}
}
