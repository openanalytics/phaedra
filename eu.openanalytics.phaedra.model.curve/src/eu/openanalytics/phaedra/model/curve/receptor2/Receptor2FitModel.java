package eu.openanalytics.phaedra.model.curve.receptor2;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.LogMolar;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.statet.rj.data.RDataFrame;
import org.eclipse.statet.rj.data.RList;
import org.eclipse.statet.rj.data.RLogicalStore;
import org.eclipse.statet.rj.data.RNumericStore;
import org.eclipse.statet.rj.servi.RServi;
import org.eclipse.statet.rj.services.FunctionCall;
import org.eclipse.statet.rj.services.util.Graphic;

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.RealValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.special.ConcentrationLogPNamedValueDescription;
import eu.openanalytics.phaedra.base.r.rservi.CairoPdfGraphic;
import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.model.curve.AbstractCurveFitModel;
import eu.openanalytics.phaedra.model.curve.CurveFitErrorCode;
import eu.openanalytics.phaedra.model.curve.CurveFitException;
import eu.openanalytics.phaedra.model.curve.CurveFitInput;
import eu.openanalytics.phaedra.model.curve.CurveFitSettings;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Definition;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

//TODO Show biological outliers as red crosses?
//TODO Use plate bounds: double[] plateBounds = CurveUtils.calculateBounds(output);
//TODO Support pICx
public class Receptor2FitModel extends AbstractCurveFitModel {

	private static final int MIN_SAMPLES_FOR_FIT = 4;
	private static final String PACKAGE_NAME = "receptor2";
	
	public static final String MODEL_ID = PACKAGE_NAME;
	
	private static final Definition[] IN_PARAMS = {
			new Definition(new RealValueDescription("Fixed Bottom", Curve.class)),
			new Definition(new RealValueDescription("Fixed Top", Curve.class)),
			new Definition(new RealValueDescription("Fixed Slope", Curve.class)),
			
			new Definition(new RealValueDescription("Confidence Level", Curve.class)),

			new Definition(new StringValueDescription("Method", Curve.class),
					null, false, null, new CurveParameter.ParameterValueList("mean", "median")),
	};

	private static final Definition[] OUT_PARAMS = {
			new Definition(new ConcentrationLogPNamedValueDescription("pIC50", Curve.class, LogMolar), null, true, null, null),
			new Definition(new RealValueDescription("pIC50 StdErr", Curve.class)),
			new Definition(new RealValueDescription("Bottom", Curve.class)),
			new Definition(new RealValueDescription("Top", Curve.class)),
			new Definition(new RealValueDescription("Slope", Curve.class)),
			new Definition(new RealValueDescription("eMin", Curve.class)),
			new Definition(new ConcentrationValueDescription("eMin Conc", Curve.class, LogMolar)),
			new Definition(new RealValueDescription("eMax", Curve.class)),
			new Definition(new ConcentrationValueDescription("eMax Conc", Curve.class, LogMolar)),
			new Definition(new ConcentrationLogPNamedValueDescription("pIC20", Curve.class, LogMolar)),
			new Definition(new ConcentrationLogPNamedValueDescription("pIC80", Curve.class, LogMolar)),
			new Definition(new RealValueDescription("Residual Variance", Curve.class)),
			new Definition(new StringValueDescription("Warning", Curve.class)),
	};
	
	private static final List<Definition> OUT_PARAMS_LIST = Arrays.asList(OUT_PARAMS);
	private static final List<Definition> OUT_KEY_PARAMS_LIST = Arrays.asList(OUT_PARAMS_LIST.stream().filter((def) -> def.key).toArray(Definition[]::new));
	
	@Override
	public String getDescription() {
		return "Fits a logisitic model using the receptor2 package";
	}

	@Override
	public Definition[] getInputParameters() {
		return IN_PARAMS;
	}

	@Override
	public List<Definition> getOutputParameters(CurveFitSettings fitSettings) {
		return OUT_PARAMS_LIST;
	}
	
	@Override
	public List<Definition> getOutputKeyParameters() {
		return OUT_KEY_PARAMS_LIST;
	}
	
	@Override
	public CurveFitErrorCode[] getErrorCodes() {
		return new CurveFitErrorCode[0];
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
			rServi.evalVoid(String.format("library(%s)", PACKAGE_NAME), null);
			
			String version = rServi.evalData(String.format("packageDescription(\"%s\")$Version", PACKAGE_NAME), null).getData().getChar(0);
			output.setFitVersion(version);
			
			int[] validIndices = IntStream.range(0, input.getConcs().length)
					.filter(i -> input.getValid()[i] && !Double.isNaN(input.getValues()[i]))
					.toArray();
			double[] concs = Arrays.stream(validIndices).mapToDouble(i -> input.getConcs()[i]).toArray();
			double[] values = Arrays.stream(validIndices).mapToDouble(i -> input.getValues()[i]).toArray();
			
			Value[] inParams = input.getSettings().getExtraParameters();
			Value[] outParams = getOutputParameters(input.getSettings()).stream()
					.map(def -> new Value(def, null, Double.NaN, null))
					.toArray(i -> new Value[i]);
			output.setOutputParameters(outParams);
			
			double fixedBottom = CurveParameter.find(inParams, "Fixed Bottom").numericValue;
			double fixedTop = CurveParameter.find(inParams, "Fixed Top").numericValue;
			double fixedSlope = CurveParameter.find(inParams, "Fixed Slope").numericValue;
			double confLevel = CurveParameter.find(inParams, "Confidence Level").numericValue;
			String method = CurveParameter.find(inParams, "Method").stringValue;
			String responseName = output.getFeature().getDisplayName();
			
			rServi.assignData("dose", RUtils.makeNumericRVector(concs), null);
			rServi.assignData("response", RUtils.makeNumericRVector(values), null);
			rServi.evalData("inputData <- data.frame(dose, response)", null);
			
			RList results = (RList)rServi.evalData(
					"value <- fittingLogisticModel("
					+ "inputData = inputData,"
					+ "fixedBottom = " + (Double.isNaN(fixedBottom) ? "NA" : fixedBottom) + ","
					+ "fixedTop = " + (Double.isNaN(fixedTop) ? "NA" : fixedTop) + ","
					+ "fixedSlope = " + (Double.isNaN(fixedSlope) ? "NA" : fixedSlope) + ","
					+ "confLevel = " + (Double.isNaN(confLevel) ? "0.95" : confLevel) + ","
					+ "robustMethod = '" + method + "',"
					+ "responseName = '" + responseName + "')", null);
			
			CurveParameter.find(outParams, "pIC50").numericValue = results.get("validpIC50").getData().getNum(0);
			CurveParameter.find(outParams, "pIC50 StdErr").numericValue = results.get("validpIC50").getData().getNum(1);
			
			RDataFrame rangeResults = (RDataFrame) results.get("rangeResults");
			if (rangeResults != null) {
				CurveParameter.find(outParams, "eMin").numericValue = rangeResults.get("response").getData().getNum(0);
				CurveParameter.find(outParams, "eMax").numericValue = rangeResults.get("response").getData().getNum(1);
				CurveParameter.find(outParams, "eMin Conc").numericValue = rangeResults.get("dose").getData().getNum(0);
				CurveParameter.find(outParams, "eMax Conc").numericValue = rangeResults.get("dose").getData().getNum(1);
			}
			CurveParameter.find(outParams, "pIC20").numericValue = results.get("validpIC20").getData().getNum(0);
			CurveParameter.find(outParams, "pIC80").numericValue = results.get("validpIC80").getData().getNum(0);
			
			Object data = results.get("modelCoefs").getData();
			if (data instanceof RLogicalStore) {
				// Fit failed, no coefs available.
			} else {
				RNumericStore coefs = (RNumericStore) data;
				CurveParameter.find(outParams, "Slope").numericValue = coefs.getNum(0);
				CurveParameter.find(outParams, "Bottom").numericValue = coefs.getNum(1);
				CurveParameter.find(outParams, "Top").numericValue = coefs.getNum(2);
			}
			
			CurveParameter.find(outParams, "Residual Variance").numericValue = RUtils.getDoubleFromList(results, "residulaVariance");
			CurveParameter.find(outParams, "Warning").stringValue = RUtils.getStringFromList(results, "warningFit");
			
			rServi.evalVoid("library(Cairo)", null);
			CairoPdfGraphic graphic = new CairoPdfGraphic();
			// Pdf inch size doesn't matter since we use vectorformat.
			graphic.setSize(4, 4, Graphic.UNIT_IN);
			
			FunctionCall plotFun = rServi.createFunctionCall("plot");
			plotFun.add("value$plot");
			byte[] plot = graphic.create(plotFun, rServi, null);
			output.setPlot(plot);
			
			output.setErrorCode(0);
		} catch (CoreException e) {
			throw new CurveFitException(e.getMessage(), e);
		} finally {
			if (rServi != null) {
				RService.getInstance().closeSession(rServi);
			}
		}
	}

}
