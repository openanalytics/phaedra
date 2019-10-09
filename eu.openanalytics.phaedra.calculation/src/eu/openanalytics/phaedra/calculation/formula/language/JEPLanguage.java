package eu.openanalytics.phaedra.calculation.formula.language;

import java.util.Map;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.scripting.jep.JEPScriptEngine;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class JEPLanguage extends BaseLanguage {

public static final String ID = "jep";
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getLabel() {
		return "JEP";
	}

	@Override
	public void validateFormula(CalculationFormula formula) throws CalculationException {
		super.validateFormula(formula);
		if (formula.getScope() == Scope.PerPlate.getCode()) throw new CalculationException("JEP cannot evaluate a formula per-plate. Please select per-well instead.");
	}

	@Override
	protected Map<String, Object> buildContext(CalculationFormula formula, IValueObject inputValue, IFeature feature) {
		Map<String, Object> context = super.buildContext(formula, inputValue, feature);
		Well well = (Well) inputValue;
		context.put(JEPScriptEngine.CONTEXT_DATA_OBJECT, well);
		//TODO
		context.put("threshold", 0.5);
		return context;
	}
	
	@Override
	protected void transformFormulaOutput(IValueObject inputValue, Object outputValue, CalculationFormula formula, Map<String, Object> context, double[] outputArray) {
		Well well = (Well) inputValue;
		int index = PlateUtils.getWellNr(well) - 1;
		outputArray[index] = getAsDouble(outputValue);
	}
}
