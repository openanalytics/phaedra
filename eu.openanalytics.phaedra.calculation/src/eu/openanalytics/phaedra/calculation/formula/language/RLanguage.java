package eu.openanalytics.phaedra.calculation.formula.language;

import java.util.Map;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;

public class RLanguage extends BaseLanguage {

public static final String ID = "r";
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getLabel() {
		return "R";
	}

	@Override
	public String generateExampleFormulaBody(CalculationFormula formula) {
		return "inputValues * 100";
	}
	
	@Override
	public void validateFormula(CalculationFormula formula) throws CalculationException {
		super.validateFormula(formula);
		// Running an r node per well is very bad performance. It may also deadlock on the r session pool.
		if (formula.getScope() == Scope.PerWell.getCode()) throw new CalculationException("JEP cannot evaluate a formula per-well. Please select per-plate instead.");
	}
	
	@Override
	public void transformFormulaOutput(IValueObject inputValue, Object outputValue, CalculationFormula formula, Map<String, Object> context, double[] outputArray) {
		double[] outputValues = (double[]) outputValue;
		for (int index=0; index<outputArray.length; index++) {
			outputArray[index] = outputValues[index];
		}		
	}
}
