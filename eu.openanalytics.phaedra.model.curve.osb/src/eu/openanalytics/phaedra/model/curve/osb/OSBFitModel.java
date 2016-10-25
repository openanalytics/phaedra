package eu.openanalytics.phaedra.model.curve.osb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;

import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.servi.RServi;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.utils.Graphic;
import eu.openanalytics.phaedra.base.r.rservi.CairoPdfGraphic;
import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.curve.AbstractCurveFitModel;
import eu.openanalytics.phaedra.model.curve.CurveFitErrorCode;
import eu.openanalytics.phaedra.model.curve.CurveFitException;
import eu.openanalytics.phaedra.model.curve.CurveFitInput;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Definition;
import eu.openanalytics.phaedra.model.curve.CurveParameter.ParameterType;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.util.CurveUtils;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public class OSBFitModel extends AbstractCurveFitModel {

	private static final int MIN_SAMPLES_FOR_FIT = 4;
	
	private static final Definition[] IN_PARAMS = {
		new Definition("Method", null, false, ParameterType.String, null, new CurveParameter.ParameterValueList("OLS", "LIN", "CENS")),
		new Definition("Type", null, false, ParameterType.String, null, new CurveParameter.ParameterValueList("A", "D")),
		new Definition("Lower bound"),
		new Definition("Upper bound")
	};
	
	private static final Definition[] OUT_PARAMS = {
		new Definition("pIC50", null, true, ParameterType.Concentration, new CurveParameter.CensoredValueRenderer("pIC50 Censor"), null),
		new Definition("pIC50 Censor", null, false, ParameterType.String, null, new CurveParameter.ParameterValueList("<", ">", "~")),
		new Definition("pIC50 StdErr"),
		new Definition("pIC50 LB"),
		new Definition("pIC50 LB StdErr"),
		new Definition("pIC50 UB"),
		new Definition("pIC50 UB StdErr"),
		new Definition("pIC50 LCL"),
		new Definition("pIC50 UCL"),
		new Definition("pIC20", null, false, ParameterType.Concentration, null, null),
		new Definition("pIC80", null, false, ParameterType.Concentration, null, null),
		new Definition("eMax", null, true, ParameterType.Numeric, null, null),
		new Definition("eMax Conc", null, true, ParameterType.Concentration, null, null),
		new Definition("Hill", null, true, ParameterType.Numeric, null, null),
		new Definition("Hill StdErr"),
		new Definition("Method Fallback", null, false, ParameterType.String, null, null),
		new Definition("r2", null, true, ParameterType.Numeric, null, null),
		new Definition("AIC"),
		new Definition("BIC"),
		new Definition("DFE"),
		new Definition("SE"),
		new Definition("Confidence Band", null, false, ParameterType.Binary, null, null),
		new Definition("Weights", null, false, ParameterType.Binary, null, null)
	};
	
	private String modelId;
	
	public OSBFitModel() {
		// Default constructor
	}
	
	public OSBFitModel(String modelId) {
		this.modelId = modelId;
	}
	
	@Override
	public String getId() {
		if (modelId == null) return super.getId();
		return modelId;
	}
	
	@Override
	public String getDescription() {
		return "This model performs One-Site-Binding curve fitting.";
	}

	@Override
	public Definition[] getInputParameters() {
		return IN_PARAMS;
	}

	@Override
	public Definition[] getOutputParameters() {
		return OUT_PARAMS;
	}

	@Override
	public CurveFitErrorCode[] getErrorCodes() {
		return OSBErrorCodes.CODES;
	}

	@Override
	public void validateInput(CurveFitInput input) throws CurveFitException {
		int validCount = 0;
		for (int i = 0; i < input.getValid().length; i++) {
			if (input.getValid()[i]) validCount++;
		}
		if (validCount < MIN_SAMPLES_FOR_FIT) throw new CurveFitException(MIN_SAMPLES_FOR_FIT + " samples are required for a " + getId() + " fit");
	}

	@Override
	public void fit(CurveFitInput input, Curve output) throws CurveFitException {
		RServi rServi = null;
		try {
			rServi = RService.getInstance().createSession();
			rServi.evalVoid("library(receptor)", null); // Note: still used for pICx
			
			String version = rServi.evalData("packageDescription(\"receptor\")$Version", null).getData().getChar(0);
			output.setFitVersion(version);
			
			// Start fitting.
			int[] missing = RUtils.makeMissingIndexArray(input.getValues());
			int size = input.getConcs().length - missing.length;

			double[] concs = new double[size];
			double[] values = new double[size];
			int[] accepts = new int[size];

			int n = 0;
			for (int i = 0; i < input.getValues().length; i++) {
				if (!Double.isNaN(input.getValues()[i])) {
					concs[n] = input.getConcs()[i];
					values[n] = input.getValues()[i];
					accepts[n] = input.getValid()[i] ? 1:0;
					n++;
				}
			}
			
			Value[] inParams = input.getSettings().getExtraParameters();
			Value[] outParams = Arrays.stream(getOutputParameters()).map(def -> new Value(def, null, Double.NaN, null)).toArray(i -> new Value[i]);
			output.setOutputParameters(outParams);
			
			// Use manual bounds, or plate bounds if manual bounds are NaN.
			double[] plateBounds = CurveUtils.calculateBounds(output);
			double[] bounds = {
					Optional.ofNullable(CurveParameter.find(input.getSettings().getExtraParameters(), "Lower bound"))
						.filter(v -> !Double.isNaN(v.numericValue)).map(v -> v.numericValue).orElse(plateBounds[0]),
					Optional.ofNullable(CurveParameter.find(input.getSettings().getExtraParameters(), "Upper bound"))
						.filter(v -> !Double.isNaN(v.numericValue)).map(v -> v.numericValue).orElse(plateBounds[1]),
			};
			// The R routine does not accept NaN values (even when not used e.g. in PL4). Replace with a legal value.
			for (int i = 0; i < bounds.length; i++) {
				if (Double.isNaN(bounds[i])) bounds[i] = 0;
			}
			
			rServi.assignData("data", RUtils.makeNumericRVector(values), null);
			rServi.assignData("conc", RUtils.makeNumericRVector(concs), null);
			rServi.assignData("accept", RUtils.makeIntegerRVector(accepts), null);
			RList results = (RList)rServi.evalData(
					"VALUE <- drcFit(data,conc,accept" 
					+ ",model=\"" + getId() + "\"" 
					+ ",method=\"" + CurveParameter.find(inParams, "Method").stringValue + "\""
					+ ",type=\"" + CurveParameter.find(inParams, "Type").stringValue + "\""
					+ ",lb=" + bounds[0]
					+ ",ub=" + bounds[1]
					+ ")", null);
			
			double[] weights = null;
			RObject weight = results.get("weights");
			if (weight != null) {
				weights = new double[weight.getData().getLength()];
				for (int i=0; i<weights.length; i++) weights[i] = weight.getData().getNum(i);
			}
			CurveParameter.setBinaryValue(CurveParameter.find(outParams, "Weights"), weights);
			
			// If receptor did a fallback from OLS to LIN, take note of that fallback.
			CurveParameter.find(outParams, "Method Fallback").stringValue = RUtils.getStringFromList(results, "METHOD");
			
			if (!getId().equals("PLOTONLY")) {
				RObject pic20Obj = rServi.evalData("pICx(VALUE, x=20)", null);
				CurveParameter.find(outParams, "pIC20").numericValue = -RUtils.getDoubleFromVector(pic20Obj,0);
				RObject pic80Obj = rServi.evalData("pICx(VALUE, x=80)", null);
				CurveParameter.find(outParams, "pIC80").numericValue = -RUtils.getDoubleFromVector(pic80Obj,0);
			}
			
			output.setErrorCode(RUtils.getIntegerFromList(results, "ERROR"));
			
			String methodUsed = CurveParameter.find(inParams, "Method").stringValue;
			if (methodUsed != null && methodUsed.equals("OLS") && !getId().equals("PLOTONLY") && output.getErrorCode() == 0) {
				RObject ci = rServi.evalData("generateCIgrid(VALUE)", null);
				double[][] grid = RUtils.getDouble2DArrayFromRDataFrame((RDataFrame)ci);
				CurveParameter.setBinaryValue(CurveParameter.find(outParams, "Confidence Band"), grid);
			}
			
			CurveParameter.find(outParams, "pIC50").numericValue = RUtils.getDoubleFromList(results, "PIC50", 3);
			CurveParameter.find(outParams, "pIC50 Censor").stringValue = RUtils.getStringFromList(results, "PIC50.cens");
			CurveParameter.find(outParams, "pIC50 StdErr").numericValue = RUtils.getDoubleFromList(results, "PIC50.se", 3);
			CurveParameter.find(outParams, "pIC50 LCL").numericValue = RUtils.getDoubleFromList(results, "PIC50.LCL", 3);
			CurveParameter.find(outParams, "pIC50 UCL").numericValue = RUtils.getDoubleFromList(results, "PIC50.UCL", 3);

			CurveParameter.find(outParams, "Hill").numericValue = RUtils.getDoubleFromList(results, "HILL", 3);
			CurveParameter.find(outParams, "Hill StdErr").numericValue = RUtils.getDoubleFromList(results, "HILL.se", 3);
			
			CurveParameter.find(outParams, "pIC50 LB").numericValue = RUtils.getDoubleFromList(results, "LB", 3);
			CurveParameter.find(outParams, "pIC50 LB StdErr").numericValue = RUtils.getDoubleFromList(results, "LB.se", 3);
			CurveParameter.find(outParams, "pIC50 UB").numericValue = RUtils.getDoubleFromList(results, "UB", 3);
			CurveParameter.find(outParams, "pIC50 UB StdErr").numericValue = RUtils.getDoubleFromList(results, "UB.se", 3);

			CurveParameter.find(outParams, "r2").numericValue = RUtils.getDoubleFromList(results, "R2", 3);
			CurveParameter.find(outParams, "SE").numericValue = RUtils.getDoubleFromList(results, "se", 3);
			CurveParameter.find(outParams, "DFE").numericValue = RUtils.getDoubleFromList(results, "dfe", 3);
			CurveParameter.find(outParams, "AIC").numericValue = RUtils.getDoubleFromList(results, "AIC", 3);
			CurveParameter.find(outParams, "BIC").numericValue = RUtils.getDoubleFromList(results, "BIC", 3);

			double[] eMax = calculateEmax(output, input);
			CurveParameter.find(outParams, "eMax").numericValue = eMax[0];
			CurveParameter.find(outParams, "eMax Conc").numericValue = eMax[1];
			
			rServi.evalVoid("library(Cairo)", null);
			CairoPdfGraphic graphic = new CairoPdfGraphic();
			// Pdf inch size doesn't matter since we use vectorformat.
			graphic.setSize(4, 4, Graphic.UNIT_IN);
			
			FunctionCall plotFun = rServi.createFunctionCall("plot");
			plotFun.add("VALUE");
			plotFun.addChar("main", output.getFeature().getDisplayName());
			plotFun.addChar("ylab", output.getFeature().getNormalization());
			byte[] plot = graphic.create(plotFun, rServi, null);
			output.setPlot(plot);
			
		} catch (CoreException e) {
			throw new CurveFitException(e.getMessage(), e);
		} finally {
			if (rServi != null) {
				RService.getInstance().closeSession(rServi);
			}
		}
	}
	
	private static double[] calculateEmax(Curve curve, CurveFitInput input) {
		double emaxConcentration = Double.NaN;
		double emaxEffect = Double.NaN;

		Map<Double, List<Double>> avgPointMap = new HashMap<Double, List<Double>>();
		int points = input.getValid().length;

		for (int point = 0; point < points; point++) {
			if ((input.getValid()[point]) && (!Double.isNaN(input.getValues()[point]))) {
				if (!avgPointMap.containsKey(input.getConcs()[point])) {
					avgPointMap.put(input.getConcs()[point], new ArrayList<Double>());
				}
				List<Double> l = avgPointMap.get(input.getConcs()[point]);
				l.add(input.getValues()[point]);
			}
		}

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

		boolean ascending = CurveParameter.find(input.getSettings().getExtraParameters(), "Type").stringValue.equals("A");
		if (ascending) {
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
		
		return new double[] { emaxEffect, emaxConcentration };
	}
}