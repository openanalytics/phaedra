package eu.openanalytics.phaedra.calculation.formula.language;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.formula.model.CalculationFormula;
import eu.openanalytics.phaedra.calculation.formula.model.Scope;
import eu.openanalytics.phaedra.calculation.jep.JEPCalculation;
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
	public void evaluateFormula(CalculationFormula formula, IValueObject inputValue, IFeature feature, double[] output) throws CalculationException {
		Well well = (Well) inputValue;
		Object outputValue = JEPCalculation.evaluate(formula.getFormula(), well);
		int index = PlateUtils.getWellNr(well) - 1;
		output[index] = getAsDouble(outputValue);
	}
}
