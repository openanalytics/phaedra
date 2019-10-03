package eu.openanalytics.phaedra.calculation.formula.model;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.scripting.engine.IScriptEngine;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

/**
 * This class represents a supported calculation formula language.
 * Each supported language has a unique id.
 * An {@link IScriptEngine} must also exist with the same id, which is capable of evaluating the given formula.
 */
public interface Language {

	public String getId();
	
	public String getLabel();
	
	public void validateFormula(CalculationFormula formula) throws CalculationException;
	
	public void evaluateFormula(CalculationFormula formula, IValueObject inputValue, IFeature feature, double[] output) throws CalculationException;
	
}
