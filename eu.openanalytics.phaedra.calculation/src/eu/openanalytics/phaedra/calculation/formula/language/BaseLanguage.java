package eu.openanalytics.phaedra.calculation.formula.language;

import eu.openanalytics.phaedra.calculation.formula.model.Language;

//TODO Implement for each supported script engine
public abstract class BaseLanguage implements Language {

	protected double getAsDouble(Object outputValue) {
		double doubleValue = Double.NaN;
		if (outputValue instanceof Number) doubleValue = ((Number) outputValue).doubleValue();
		else if (outputValue != null) doubleValue = Double.parseDouble(String.valueOf(outputValue));
		return doubleValue;
	}
}
