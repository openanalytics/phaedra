package eu.openanalytics.phaedra.calculation.formula.language;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.formula.FormulaUtils;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.InputType;
import eu.openanalytics.phaedra.calculation.formula.model.Language;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public abstract class BaseLanguage implements Language {

	@Override
	public void validateFormula(CalculationFormula formula) throws CalculationException {
		// Default: always valid.
	}

	@Override
	public void evaluateFormula(CalculationFormula formula, IValueObject inputValue, IFeature feature, double[] output, Map<String, Object> params) throws CalculationException {
		Map<String, Object> context = buildContext(formula, inputValue, feature);
		if (params != null) context.putAll(params);
		
		Object outputValue = null;
		try {
			outputValue = ScriptService.getInstance().executeScript(formula.getFormula(), context, getId());
		} catch (ScriptException e) {
			throw new CalculationException("Formula evaluation failed", e);
		}
		transformFormulaOutput(inputValue, outputValue, formula, context, output);	
	}
	
	protected Map<String, Object> buildContext(CalculationFormula formula, IValueObject inputValue, IFeature feature) {
		Map<String, Object> context = new HashMap<>();
		InputType type = FormulaUtils.getInputType(formula);
		context.put("featureId", feature.getId());
		
		if (inputValue instanceof Well) {
			Well well = (Well) inputValue;
			context.put("plateId", well.getPlate().getId());
			context.put("wellId", well.getId());
			context.put("inputValue", type.getInputValue(well, feature));
		} else if (inputValue instanceof Plate) {
			Plate plate = (Plate) inputValue;
			context.put("plateId", plate.getId());
			Well[] wells = PlateService.streamableList(plate.getWells())
					.stream()
					.sorted(PlateUtils.WELL_NR_SORTER)
					.toArray(i -> new Well[i]);
			long[] wellIds = Arrays.stream(wells).mapToLong(w -> w.getId()).toArray();
			double[] inputValues = Arrays.stream(wells).mapToDouble(w -> type.getInputValue(w, feature)).toArray();
			context.put("wellIds", wellIds);
			context.put("inputValues", inputValues);
		} else {
			throw new IllegalArgumentException(String.format("Formula language %s does not support input type %s", getLabel(), inputValue.getClass()));
		}
		
		return context;
	}

	protected void transformFormulaOutput(IValueObject inputValue, Object outputValue, CalculationFormula formula, Map<String, Object> context, double[] outputArray) {
		throw new UnsupportedOperationException(String.format("The method 'transformFormulaOutput' is not implemented for %s", this.getClass().getName()));
	}
	
	protected double getAsDouble(Object outputValue) {
		double doubleValue = Double.NaN;
		if (outputValue instanceof Number) {
			doubleValue = ((Number) outputValue).doubleValue();
		} else if (outputValue instanceof Boolean) {
			doubleValue = ((Boolean) outputValue) ? 1.0 : 0.0;
		} else if (outputValue != null) {
			try {
				doubleValue = Double.parseDouble(String.valueOf(outputValue));
			} catch (NumberFormatException e) {
				throw new CalculationException("Invalid or non-numeric output: " + outputValue);
			}
		}
		return doubleValue;
	}
}
