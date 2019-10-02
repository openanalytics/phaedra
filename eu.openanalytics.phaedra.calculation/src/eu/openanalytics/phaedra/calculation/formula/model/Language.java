package eu.openanalytics.phaedra.calculation.formula.model;

import java.util.Map;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.scripting.engine.IScriptEngine;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * This class represents a supported calculation formula language.
 * Each supported language has a unique id.
 * An {@link IScriptEngine} must also exist with the same id, which is capable of evaluating the given formula.
 */
public interface Language {

	public String getId();
	
	public String getLabel();
	
	public Map<String, Object> buildContext(IValueObject inputValue, CalculationFormula formula, Plate plate, Feature feature);
	public void transformFormulaOutput(IValueObject inputValue, Object outputValue, CalculationFormula formula, Map<String, Object> context, double[] outputArray);
}
