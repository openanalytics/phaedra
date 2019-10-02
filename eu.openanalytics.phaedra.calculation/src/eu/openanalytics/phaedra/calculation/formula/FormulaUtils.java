package eu.openanalytics.phaedra.calculation.formula;

import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.InputType;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;

public class FormulaUtils {

	public static Scope getScope(CalculationFormula formula) {
		return Scope.get(formula.getScope());
	}
	
	public static InputType getInputType(CalculationFormula formula) {
		return InputType.get(formula.getInputType());
	}
	
}
