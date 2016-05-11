package eu.openanalytics.phaedra.model.curve.fit;

import java.io.IOException;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;

import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.servi.RServi;
import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.model.curve.CurveService.CurveKind;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.curve.vo.PLACCurve;

public class PLACCurveFitter extends BaseCurveFitter {
	
	@Override
	public CurveKind getSupportedKind() {
		return CurveKind.PLAC;
	}

	protected void doFitCurve(Curve curve, CurveDataPoints fitData) throws CurveFitException {
		PLACCurve plac = (PLACCurve)curve;
		
		RServi rServi = null;
		try {
			rServi = RService.getInstance().createSession();
			rServi.evalVoid("library(miscFitters)", null);
			
			// Obtain version of fitting package.
			RObject v = rServi.evalData("packageDescription(\"miscFitters\")$Version", null);
			String version = v.getData().getChar(0);
			plac.setFitVersion(version);
			plac.setFitDate(new Date());
			
			// Define helper functions.
			String f = "makeDs <- function(concs, responses, accepts){ \n"
					+ "res <- vector(mode = \"list\", length = 3) \n" 
					+ "names(res) <- paste(\"test\", 1:3, sep= \"\") \n"
					+ "test1 <- data.frame(conc = concs, response = responses, accept = accepts) \n" 
					+ "res <- test1 \n" 
					+ "return(res) \n" + "} \n";
			rServi.evalVoid(f, null);
			f = "makeBound <- function(mean, sd){ \n" 
					+ "res <- c(mean, sd) \n" 
					+ "if (length(res)) names(res) <- c(\"mean\", \"sd\") \n" 
					+ "return(res) \n" + "} \n";
			rServi.evalVoid(f, null);
			
			// Start fitting.
			rServi.evalVoid("lc <- makeBound(mean=" + fitData.lcMean + ",sd=" + fitData.lcStdev + ")", null);
			rServi.evalVoid("hc <- makeBound(mean=" + fitData.hcMean + ",sd=" + fitData.hcStdev + ")", null);
			
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

			rServi.assignData("concs", RUtils.makeNumericRVector(concs), null);
			rServi.assignData("responses", RUtils.makeNumericRVector(values), null);
			rServi.assignData("accepts", RUtils.makeIntegerRVector(accepts), null);
			rServi.evalVoid("ds <- makeDs(concs,responses,accepts)", null);
			
			RList results = (RList)rServi.evalData("VALUE <- pLacFit("
					+ "model=\"" + plac.getSettings().getModel() + "\""
					+ ",method=\"" + plac.getSettings().getMethod() + "\""
					+ ",type=\"" + plac.getSettings().getType() + "\""
					+ ",setting=" + plac.getSettings().getThreshold()
					+ ",dataset=ds, lc=lc, hc=hc)", null);

			String error = RUtils.getStringFromList(results, "ERROR");
			plac.setFitError(Integer.valueOf(error));

			plac.setPlacCensor(RUtils.getStringFromList(results, "PLACCENS"));
			plac.setPlac(RUtils.getDoubleFromList(results, "PLAC", 3));
			plac.setThreshold(RUtils.getDoubleFromList(results, "THRESHOLD", 3));

			plac.setLcMean(fitData.lcMean);
			plac.setLcStdev(fitData.lcStdev);
			plac.setHcMean(fitData.hcMean);
			plac.setHcStdev(fitData.hcStdev);
			
			plac.setNic(RUtils.getIntegerFromList(results, "NIC"));
			plac.setNac(RUtils.getIntegerFromList(results, "NAC"));
			
			try {
				plac.setPlot(getPlotImage(rServi, plac, "conc[M]"));
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
