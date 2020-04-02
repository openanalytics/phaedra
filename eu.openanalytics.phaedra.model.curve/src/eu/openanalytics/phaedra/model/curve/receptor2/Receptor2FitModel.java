package eu.openanalytics.phaedra.model.curve.receptor2;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.LogMolar;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.statet.rj.data.RList;
import org.eclipse.statet.rj.servi.RServi;
import org.eclipse.statet.rj.services.FunctionCall;
import org.eclipse.statet.rj.services.util.Graphic;

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

public class Receptor2FitModel extends AbstractCurveFitModel {

	private static final int MIN_SAMPLES_FOR_FIT = 4; //TODO check with Vahid
	private static final String PACKAGE_NAME = "receptor2";
	
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
			new Definition(new RealValueDescription("Residual Variance", Curve.class)),
			new Definition(new StringValueDescription("Warning PD", Curve.class)),
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
			
			//TODO filter out rejected values?
			//TODO double[] plateBounds = CurveUtils.calculateBounds(output);
			rServi.assignData("dose", RUtils.makeNumericRVector(concs), null);
			rServi.assignData("response", RUtils.makeNumericRVector(values), null);
			rServi.evalData("inputData <- data.frame(dose, response)", null);
			
			RList results = (RList)rServi.evalData(
					"value <- fittingLogisticModel("
					+ "inputData = inputData,"
					+ "fixedBottom = NA,"
					+ "fixedTop = NA,"
					+ "fixedSlope = NA,"
					+ "confLevel = 0.95,"
					+ "robustMethod = 'mean',"
					+ "responseName = 'Effect')", null);
			
			//TODO pICx ?

			CurveParameter.find(outParams, "pIC50").numericValue = RUtils.getDoubleFromList(results, "validpIC50", 3);
			CurveParameter.find(outParams, "Residual Variance").numericValue = RUtils.getDoubleFromList(results, "residualVariance", 3);
			CurveParameter.find(outParams, "Warning PD").stringValue = RUtils.getStringFromList(results, "warningPD");
			
			rServi.evalVoid("library(Cairo)", null);
			CairoPdfGraphic graphic = new CairoPdfGraphic();
			// Pdf inch size doesn't matter since we use vectorformat.
			graphic.setSize(4, 4, Graphic.UNIT_IN);
			
			FunctionCall plotFun = rServi.createFunctionCall("plot");
			plotFun.add("value$plot");
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

}
