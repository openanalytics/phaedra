package eu.openanalytics.phaedra.calculation.formula.language;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.calculation.formula.FormulaUtils;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.InputType;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class JavaScriptLanguage extends BaseLanguage {

	@Override
	public String getId() {
		return "javaScript";
	}

	@Override
	public String getLabel() {
		return "JavaScript";
	}

	@Override
	public Map<String, Object> buildContext(IValueObject inputValue, CalculationFormula formula, Plate plate, Feature feature) {
		Map<String, Object> context = new HashMap<>();
		context.put("featureId", feature.getId());
		context.put("feature", feature);
		context.put("plateId", plate.getId());
		context.put("plate", plate);
		
		InputType type = FormulaUtils.getInputType(formula);
		
		if (inputValue instanceof Well) {
			Well well = (Well) inputValue;
			context.put("wellId", well.getId());
			context.put("well", well);			
			context.put("inputValue", type.getInputValue(well, feature));
		} else if (inputValue instanceof Plate) {
			Well[] wells = PlateService.streamableList(plate.getWells())
					.stream()
					.sorted(PlateUtils.WELL_NR_SORTER)
					.toArray(i -> new Well[i]);
			long[] wellIds = Arrays.stream(wells).mapToLong(w -> w.getId()).toArray();
			double[] inputValues = Arrays.stream(wells).mapToDouble(w -> type.getInputValue(w, feature)).toArray();
			context.put("wellIds", wellIds);
			context.put("wells", wells);
			context.put("inputValues", inputValues);
			context.put("outputValues", new double[inputValues.length]);
		} else {
			throw new IllegalArgumentException(String.format("Formula language %s does not support input type %s", getLabel(), inputValue.getClass()));
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
			Plate plate = (Plate) inputValue;
			double[] outputValues = (double[]) context.get("outputValues");
			for (index=0; index<outputArray.length; index++) {
				well = PlateUtils.getWell(plate, index + 1);
				outputArray[index] = outputValues[index];
			}
		}
	}
}
