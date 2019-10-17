package eu.openanalytics.phaedra.calculation.formula.language;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.formula.FormulaUtils;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.InputType;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class JavaScriptLanguage extends BaseLanguage {

	public static final String ID = "javaScript";
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getLabel() {
		return "JavaScript";
	}

	@Override
	public String generateExampleFormulaBody(CalculationFormula formula) {
		Scope scope = Scope.get(formula.getScope());
		switch (scope) {
		case PerPlate:
			return "for (var i in inputValues) { outputValues[i] = inputValues[i] * 100 }";
		case PerWell:
		default:
			return "inputValue * 100";
		}
	}
	
	@Override
	protected Map<String, Object> buildContext(CalculationFormula formula, IValueObject inputValue, IFeature feature) {
		Map<String, Object> context = super.buildContext(formula, inputValue, feature);
		context.put("feature", feature);
		
		context.put("getValue", new BiFunction<Well, String, Double>() {
			@Override
			public Double apply(Well w, String featureName) {
				Feature f = ProtocolUtils.getFeatureByName(featureName, ProtocolUtils.getProtocolClass(w));
				if (f == null) return Double.NaN;
				return CalculationService.getInstance().getAccessor(w.getPlate()).getNumericValue(w, f, null);
			}
		});
		
		InputType type = FormulaUtils.getInputType(formula);
		if (inputValue instanceof Well) {
			Well well = (Well) inputValue;
			context.put("plate", well.getPlate());
			context.put("well", well);			
		} else if (inputValue instanceof Plate) {
			Plate plate = (Plate) inputValue;
			context.put("plate", plate);
			Well[] wells = PlateService.streamableList(plate.getWells())
					.stream()
					.sorted(PlateUtils.WELL_NR_SORTER)
					.toArray(i -> new Well[i]);
			double[] inputValues = Arrays.stream(wells).mapToDouble(w -> type.getInputValue(w, feature)).toArray();
			context.put("wells", wells);
			context.put("outputValues", new double[inputValues.length]);
		}
		
		return context;
	}

	@Override
	public void transformFormulaOutput(IValueObject inputValue, Object outputValue, CalculationFormula formula, Map<String, Object> context, double[] outputArray) {
		switch (FormulaUtils.getScope(formula)) {
		case PerWell:
			Well well = (Well) inputValue;
			int index = PlateUtils.getWellNr(well) - 1;
			outputArray[index] = getAsDouble(outputValue);
			break;
		case PerPlate:
		default:
			double[] outputValues = (double[]) context.get("outputValues");
			for (index=0; index<outputArray.length; index++) {
				outputArray[index] = outputValues[index];
			}
		}
	}
}
