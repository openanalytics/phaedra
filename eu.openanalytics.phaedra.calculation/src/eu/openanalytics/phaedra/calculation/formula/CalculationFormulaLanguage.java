package eu.openanalytics.phaedra.calculation.formula;

import eu.openanalytics.phaedra.base.scripting.engine.IScriptEngine;

/**
 * This class represents a supported calculation formula language.
 * Each supported language has a unique id.
 * An {@link IScriptEngine} must also exist with the same id, which is capable of evaluating the given formula.
 */
public interface CalculationFormulaLanguage {

	public String getId();
	
	public String getLabel();
	
	public static CalculationFormulaLanguage[] getLanguages() {
		return null;
	}
	
	public static CalculationFormulaLanguage get(String id) {
		return null;
	}
	
}
