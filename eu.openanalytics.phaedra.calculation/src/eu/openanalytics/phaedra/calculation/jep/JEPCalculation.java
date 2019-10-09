package eu.openanalytics.phaedra.calculation.jep;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;

import eu.openanalytics.phaedra.base.scripting.api.ScriptService;
import eu.openanalytics.phaedra.base.scripting.jep.JEPScriptEngine;
import eu.openanalytics.phaedra.calculation.CalculationException;

public class JEPCalculation {

	/**
	 * Evaluate an expression into a single numeric value.
	 * If the expression does not return a single numeric value (e.g. an array), NaN is returned instead.
	 */
	public static float evaluate(String expression, Object data) throws CalculationException {
		Object result = eval(expression, data);
		if (result instanceof Float) return (Float) result;
		else return Float.NaN;
	}
	
	public static float evaluate(String expression, Map<String, Object> context) throws CalculationException {
		Object result = eval(expression, context);
		if (result instanceof Float) return (Float) result;
		else return Float.NaN;
	}

	/**
	 * Evaluate an expression into a numeric array
	 * If the expression returns a single value, it is wrapped into an array.
	 */
	public static float[] evaluateArray(String expression, Object data) throws CalculationException {
		Object result = eval(expression, data);
		float[] values;
		if (result instanceof Float) values = new float[] { (Float) result };
		else if (result instanceof float[]) values = (float[]) result;
		else values = new float[0];
		return values;
	}

	private static Object eval(String expression, Object dataObject) throws CalculationException {
		Map<String, Object> context = new HashMap<>();
		context.put(JEPScriptEngine.CONTEXT_DATA_OBJECT, dataObject);
		return eval(expression, context);
	}
	
	private static Object eval(String expression, Map<String, Object> context) throws CalculationException {
		try {
			return ScriptService.getInstance().executeScript(expression, context, "jep");
		} catch (ScriptException e) {
			throw new CalculationException(e);
		}
	}

}