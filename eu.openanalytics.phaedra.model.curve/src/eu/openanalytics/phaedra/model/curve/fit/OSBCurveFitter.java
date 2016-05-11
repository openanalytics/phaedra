package eu.openanalytics.phaedra.model.curve.fit;

import java.io.IOException;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;

import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.servi.RServi;
import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveKind;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveMethod;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveModel;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.OSBCurve;

public class OSBCurveFitter extends BaseCurveFitter {

	@Override
	public CurveKind getSupportedKind() {
		return CurveKind.OSB;
	}

	protected void doFitCurve(Curve curve, CurveDataPoints fitData) throws CurveFitException {
		OSBCurve osb = (OSBCurve)curve;
		RServi rServi = null;
		try {
			String packageName = "receptor";
			rServi = RService.getInstance().createSession();
			rServi.evalVoid("library(" + packageName + ")", null); // Note: still used for pICx
			
			// Obtain version of fitting package.
			RObject v = rServi.evalData("packageDescription(\"" + packageName + "\")$Version", null);
			String version = v.getData().getChar(0);
			osb.setFitVersion(packageName + " " + version);
			osb.setFitDate(new Date());
			
			// Start fitting.
			int[] missing = RUtils.makeMissingIndexArray(fitData.values);
			int size = fitData.concs.length - missing.length;

			double[] concs = new double[size];
			double[] values = new double[size];
			int[] accepts = new int[size];

			int n = 0;
			for (int i = 0; i < fitData.concs.length; i++) {
				if (!Double.isNaN(fitData.values[i])) {
					concs[n] = fitData.concs[i];
					values[n] = fitData.values[i];
					accepts[n] = fitData.accepts[i];
					n++;
				}
			}
			
			// Use manual bounds, or plate bounds if manual bounds are NaN.
			double manualLb = curve.getSettings().getLb();
			double plateLb = fitData.lcMedian;
			if (!Double.isNaN(fitData.hcMedian)) plateLb = Math.min(fitData.lcMedian, fitData.hcMedian);
			double lb = (Double.isNaN(manualLb)) ? plateLb : manualLb;
			double manualUb = curve.getSettings().getUb();
			double plateUb = fitData.hcMedian;
			if (!Double.isNaN(fitData.lcMedian)) plateUb = Math.max(fitData.lcMedian, fitData.hcMedian);
			double ub = (Double.isNaN(manualUb)) ? plateUb : manualUb;

			// The R routine does not accept NaN values. Replace with a legal value.
			// E.g. for PL4, it doesn't matter but R still checks the lb and ub values and may error on NaN.
			if (Double.isNaN(lb)) lb = 0;
			if (Double.isNaN(ub)) ub = 0;
			
			rServi.assignData("data", RUtils.makeNumericRVector(values), null);
			rServi.assignData("conc", RUtils.makeNumericRVector(concs), null);
			rServi.assignData("accept", RUtils.makeIntegerRVector(accepts), null);
			RList results = (RList)rServi.evalData(
					"VALUE <- drcFit(data,conc,accept" 
					+ ",model=\"" + osb.getSettings().getModel() + "\"" 
					+ ",method=\"" + osb.getSettings().getMethod() + "\""
					+ ",type=\"" + osb.getSettings().getType() + "\""
					+ ",lb=" + lb
					+ ",ub=" + ub
					+ ")", null);
			
			double[] weights = null;
			RObject weight = results.get("weights");
			if (weight != null) {
				weights = new double[weight.getData().getLength()];
				for (int i=0; i<weights.length; i++) weights[i] = weight.getData().getNum(i);
			}
			osb.setWeights(weights);
			
			// If receptor did a fallback from OLS to LIN, take note of that fallback.
			if (osb.getSettings().getMethod().equals(CurveMethod.OLS.toString())) {
				String methodUsed = RUtils.getStringFromList(results, "METHOD");
				if (!methodUsed.isEmpty()) osb.getSettings().setMethod(methodUsed);
			}
			
			if (osb.getSettings().getModel().equals(CurveModel.PLOTONLY.name())) {
				osb.setPic20(Double.NaN);
				osb.setPic80(Double.NaN);
			} else {
				RObject pic20Obj = rServi.evalData("pICx(VALUE, x=20)", null);
				RObject pic80Obj = rServi.evalData("pICx(VALUE, x=80)", null);
				osb.setPic20(-RUtils.getDoubleFromVector(pic20Obj,0));
				osb.setPic80(-RUtils.getDoubleFromVector(pic80Obj,0));
			}
			
			osb.setFitError(RUtils.getIntegerFromList(results, "ERROR"));
			
			if (osb.getSettings().getMethod().equals(CurveMethod.OLS.name())
					&& !osb.getSettings().getModel().equals(CurveModel.PLOTONLY.name())
					&& osb.getFitError() == 0) {
				RObject ci = rServi.evalData("generateCIgrid(VALUE)", null);
				double[][] grid = RUtils.getDouble2DArrayFromRDataFrame((RDataFrame)ci);
				osb.setCiGrid(grid);
			}
			
			osb.setLb(RUtils.getDoubleFromList(results, "LB", 3));
			osb.setLbStdErr(RUtils.getDoubleFromList(results, "LB.se", 3));
			osb.setUb(RUtils.getDoubleFromList(results, "UB", 3));
			osb.setUbStdErr(RUtils.getDoubleFromList(results, "UB.se", 3));

			osb.setPlateLb(plateLb);
			osb.setPlateUb(plateUb);
			
			osb.setSe(RUtils.getDoubleFromList(results, "se", 3));
			osb.setDfe(RUtils.getDoubleFromList(results, "dfe", 3));
			osb.setAic(RUtils.getDoubleFromList(results, "AIC", 3));
			osb.setBic(RUtils.getDoubleFromList(results, "BIC", 3));
			osb.setR2(RUtils.getDoubleFromList(results, "R2", 3));

			osb.setPic50(RUtils.getDoubleFromList(results, "PIC50", 3));
			osb.setPic50Censor(RUtils.getStringFromList(results, "PIC50.cens"));
			osb.setPic50StdErr(RUtils.getDoubleFromList(results, "PIC50.se", 3));
			osb.setPic50Lcl(RUtils.getDoubleFromList(results, "PIC50.LCL", 3));
			osb.setPic50Ucl(RUtils.getDoubleFromList(results, "PIC50.UCL", 3));

			osb.setHill(RUtils.getDoubleFromList(results, "HILL", 3));
			osb.setHillStdErr(RUtils.getDoubleFromList(results, "HILL.se", 3));

			try {
				osb.setPlot(getPlotImage(rServi, osb, null));
			} catch (IOException e) {
				throw new CurveFitException("Failed to create curve plot", e);
			}
			
		} catch (CoreException e) {
			throw new CurveFitException(e.getMessage(), e);
		} finally {
			if (rServi != null) {
				RService.getInstance().closeSession(rServi);
			}
		}
	}
}
