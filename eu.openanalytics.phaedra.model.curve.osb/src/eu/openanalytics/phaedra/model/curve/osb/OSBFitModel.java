package eu.openanalytics.phaedra.model.curve.osb;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.LogMolar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.databinding.conversion.IConverter;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.statet.rj.data.RDataFrame;
import org.eclipse.statet.rj.data.RList;
import org.eclipse.statet.rj.data.RObject;
import org.eclipse.statet.rj.servi.RServi;
import org.eclipse.statet.rj.services.FunctionCall;
import org.eclipse.statet.rj.services.util.Graphic;

import eu.openanalytics.phaedra.base.datatype.description.ByteArrayDescription;
import eu.openanalytics.phaedra.base.datatype.description.ConcentrationValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.RealValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.special.ConcentrationLogPNamedCensorDescription;
import eu.openanalytics.phaedra.base.datatype.special.ConcentrationLogPNamedCensoredValueDescription;
import eu.openanalytics.phaedra.base.datatype.special.ConcentrationLogPNamedValueDescription;
import eu.openanalytics.phaedra.base.datatype.special.RealValueConcentrationLogPNamedDescription;
import eu.openanalytics.phaedra.base.r.rservi.CairoPdfGraphic;
import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.calculation.stat.StatService;
import eu.openanalytics.phaedra.model.curve.AbstractCurveFitModel;
import eu.openanalytics.phaedra.model.curve.CurveFitErrorCode;
import eu.openanalytics.phaedra.model.curve.CurveFitException;
import eu.openanalytics.phaedra.model.curve.CurveFitInput;
import eu.openanalytics.phaedra.model.curve.CurveFitSettings;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Definition;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.util.CurveUtils;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public class OSBFitModel extends AbstractCurveFitModel {

	private static final int MIN_SAMPLES_FOR_FIT = 4;
	
	
	private static final Definition[] IN_PARAMS = {
		new Definition(new StringValueDescription("Method", Curve.class),
				null, false, null, new CurveParameter.ParameterValueList("OLS", "LIN", "CENS")),
		new Definition(new StringValueDescription("Type", Curve.class),
				null, false, null, new CurveParameter.ParameterValueList("A", "D")),
		new Definition(new RealValueDescription("Lower bound", Curve.class)),
		new Definition(new RealValueDescription("Upper bound", Curve.class)),
		new Definition(new StringValueDescription("Custom ICx/pICx", Curve.class),
				"Specify comma separated percent x of additional ICx/pICx measures.", false, null, new PICxRestriction())
	};
	
	private static final Definition[] OUT_PARAMS = {
		new Definition(new ConcentrationLogPNamedCensoredValueDescription("pIC50", Curve.class, LogMolar, "pIC50 Censor"),
				null, true, new CurveParameter.CensoredValueRenderer("pIC50 Censor"), null),
		new Definition(new ConcentrationLogPNamedCensorDescription("pIC50 Censor", Curve.class, LogMolar),
				null, false, null, new CurveParameter.ParameterValueList("<", ">", "~")),
		new Definition(new RealValueDescription("pIC50 StdErr", Curve.class)),
		new Definition(new RealValueConcentrationLogPNamedDescription("pIC50 LB", Curve.class, LogMolar)),
		new Definition(new RealValueConcentrationLogPNamedDescription("pIC50 LB StdErr", Curve.class, LogMolar)),
		new Definition(new RealValueConcentrationLogPNamedDescription("pIC50 UB", Curve.class, LogMolar)),
		new Definition(new RealValueConcentrationLogPNamedDescription("pIC50 UB StdErr", Curve.class, LogMolar)),
		new Definition(new ConcentrationLogPNamedValueDescription("pIC50 LCL", Curve.class, LogMolar)),
		new Definition(new ConcentrationLogPNamedValueDescription("pIC50 UCL", Curve.class, LogMolar)),
		new Definition(new ConcentrationLogPNamedValueDescription("pIC20", Curve.class, LogMolar)),
		new Definition(new ConcentrationLogPNamedValueDescription("pIC80", Curve.class, LogMolar)),
		//PHA-652: 	
		new Definition(new RealValueDescription("eMin", Curve.class), null, true, null, null), 
		new Definition(new ConcentrationValueDescription("eMin Conc", Curve.class, LogMolar), null, true, null, null),
		new Definition(new RealValueDescription("eMax", Curve.class), null, true, null, null),
		new Definition(new ConcentrationValueDescription("eMax Conc", Curve.class, LogMolar), null, true, null, null),
		new Definition(new RealValueDescription("Hill", Curve.class), null, true, null, null),
		new Definition(new RealValueDescription("Hill StdErr", Curve.class)),
		new Definition(new StringValueDescription("Method Fallback", Curve.class)),
		new Definition(new RealValueDescription("r2", Curve.class),	null, true, null, null),
		new Definition(new RealValueDescription("AIC", Curve.class)),
		new Definition(new RealValueDescription("BIC", Curve.class)),
		new Definition(new RealValueDescription("DFE", Curve.class)),
		new Definition(new RealValueDescription("SE", Curve.class)),
		new Definition(new ByteArrayDescription("Confidence Band", Curve.class)),
		new Definition(new ByteArrayDescription("Weights", Curve.class))
	};
	private static final List<Definition> OUT_PARAMS_LIST = Arrays.asList(OUT_PARAMS);
	private static final List<Definition> OUT_KEY_PARAMS_LIST = Arrays.asList(OUT_PARAMS_LIST.stream().filter((def) -> def.key).toArray(Definition[]::new));
	
	
	private String modelId;
	private String description;
	
	public OSBFitModel() {
		// Default constructor
	}
	
	public OSBFitModel(String modelId, String description) {
		this.modelId = modelId;
		this.description = description;
	}
	
	@Override
	public String getId() {
		if (modelId == null) return super.getId();
		return modelId;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Definition[] getInputParameters() {
		return IN_PARAMS;
	}

	@Override
	public List<Definition> getOutputParameters(CurveFitSettings fitSettings) {
		if (fitSettings != null) {
			Value picxValue = CurveParameter.find(fitSettings.getExtraParameters(), "Custom pICx");
			if (picxValue != null && picxValue.stringValue != null) {
				Set<Integer> percents = parseICxParam(picxValue.stringValue);
				if (percents != null && !percents.isEmpty()) {
					percents.add(20);
					percents.add(80);
					
					Definition[] params = new Definition[OUT_PARAMS.length + percents.size() - 2];
					int insert = CurveParameter.indexOf(OUT_PARAMS_LIST, "pIC20");
					System.arraycopy(OUT_PARAMS, 0, params, 0, insert);
					int i = insert;
					for (Integer percent : percents) {
						params[i++] = new Definition(new ConcentrationLogPNamedValueDescription("pIC" + percent, Curve.class, LogMolar));
					}
					insert += 2;
					System.arraycopy(OUT_PARAMS, insert, params, i, OUT_PARAMS.length - insert);
					return Arrays.asList(params);
				}
			}
		}
		return OUT_PARAMS_LIST;
	}
	
	@Override
	public List<Definition> getOutputKeyParameters() {
		return OUT_KEY_PARAMS_LIST;
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
			Value[] outParams = getOutputParameters(input.getSettings()).stream().map(def -> new Value(def, null, Double.NaN, null)).toArray(i -> new Value[i]);
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
				weights = new double[(int) weight.getData().getLength()];
				for (int i=0; i<weights.length; i++) weights[i] = weight.getData().getNum(i);
			}
			CurveParameter.setBinaryValue(CurveParameter.find(outParams, "Weights"), weights);
			
			// If receptor did a fallback from OLS to LIN, take note of that fallback.
			CurveParameter.find(outParams, "Method Fallback").stringValue = RUtils.getStringFromList(results, "METHOD");
			
			if (!getId().equals("PLOTONLY")) {
				for (Value value : outParams) {
					String name = value.definition.name;
					if (name.startsWith("pIC") && !name.startsWith("pIC50")) {
						try {
							int p = Integer.parseInt(name.substring(3));
							FunctionCall rCall = rServi.createFunctionCall("pICx");
							rCall.add("VALUE");
							rCall.addInt("x", p);
							RObject picObj = rCall.evalData(null);
							value.numericValue = -RUtils.getDoubleFromVector(picObj, 0);
						} catch (NumberFormatException e) {}
					}
				}
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

			double[] eMinMax = calculateEMinMax(output, input);
			CurveParameter.find(outParams, "eMin").numericValue = eMinMax[0];
			CurveParameter.find(outParams, "eMin Conc").numericValue = eMinMax[1];
			CurveParameter.find(outParams, "eMax").numericValue = eMinMax[2];
			CurveParameter.find(outParams, "eMax Conc").numericValue = eMinMax[3];
			
//			if (output.getErrorCode() != -3) {
			if (size > 0) {
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
			}
		} catch (CoreException e) {
			throw new CurveFitException(e.getMessage(), e);
		} finally {
			if (rServi != null) {
				RService.getInstance().closeSession(rServi);
			}
		}
	}
	
	/** @return { eMinEffect, eMinConcentration, eMaxEffect, eMaxConcentration } */
	private static double[] calculateEMinMax(Curve curve, CurveFitInput input) {
		Map<Double, List<Double>> avgPointMap = new HashMap<Double, List<Double>>();
		int pointCount = input.getValid().length;
		for (int point = 0; point < pointCount; point++) {
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
		
		double eMinEffect = Double.NaN;
		double eMinConcentration = Double.NaN;
		double eMaxEffect = Double.NaN;
		double eMaxConcentration = Double.NaN;
		
		for (int i = 0; i < avgConcs.length; i++) {
			if (Double.isNaN(eMinEffect)) {
				eMinEffect = avgValues[i];
				eMinConcentration = avgConcs[i];
				eMaxEffect = avgValues[i];
				eMaxConcentration = avgConcs[i];
			}
			else if ((avgValues[i] < eMinEffect)) {
				eMinEffect = avgValues[i];
				eMinConcentration = avgConcs[i];
			}
			else if ((avgValues[i] > eMaxEffect)) {
				eMaxEffect = avgValues[i];
				eMaxConcentration = avgConcs[i];
			}
		}
		
		Value type = CurveParameter.find(input.getSettings().getExtraParameters(), "Type");
		boolean ascending = (type == null) ? true : type.stringValue.equals("A");
		return (ascending) ?
				new double[] { eMinEffect, eMinConcentration, eMaxEffect, eMaxConcentration } :
				new double[] { eMaxEffect, eMaxConcentration, eMinEffect, eMinConcentration };
	}
	
	
	private static Set<Integer> parseICxParam(String value) {
		try {
			Set<Integer> percents = new TreeSet<>();
			value = value.trim();
			if (!value.isEmpty()) {
				String[] strings = value.split(",");
				for (int i = 0; i < strings.length; i++) {
					String s = strings[i].trim();
					if (s.isEmpty()) continue;
					int p = Integer.parseInt(s);
					if (p > 0 && p < 100) {
						if (p != 50) percents.add(p);
					} else {
						return null;
					}
				}
			}
			return percents;
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	private static class PICxRestriction implements IValidator, IConverter {
		
		@Override
		public Object getFromType() {
			return String.class;
		}
		
		@Override
		public Object getToType() {
			return String.class;
		}
		
		@Override
		public IStatus validate(Object value) {
			if (parseICxParam((String)value) == null) {
				return ValidationStatus.error("Enter comma separated integer percent values for Custom ICx.");
			}
			return ValidationStatus.ok();
		}
		
		@Override
		public Object convert(Object fromObject) {
			Set<Integer> percents = parseICxParam((String)fromObject);
			if (percents == null) return null;
			return percents.stream().map((p) -> p.toString()).collect(Collectors.joining(", "));
		}
		
	}
	
}
