package eu.openanalytics.phaedra.calculation.jep;

import java.util.List;

import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.Scaler;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.type.Complex;

import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.jep.parse.IScanner;
import eu.openanalytics.phaedra.calculation.jep.parse.JEPParser;
import eu.openanalytics.phaedra.model.plate.vo.Well;

/**
 * This service can evaluate JEP expressions against wells.
 * The expressions may contain references to well features, subwell features and some other items.
 * See {@link IScanner} and its implementations for more information on these references.
 */
public class JEPCalculationService {

	private static JEPCalculationService instance = new JEPCalculationService();

	private JEPCalculationService() {
		// Hidden constructor.
	}

	public static JEPCalculationService getInstance() {
		return instance;
	}

	/**
	 * Evaluate an expression for a single well.
	 * If the expression does not return a single numeric value (e.g. an array), NaN is returned instead.
	 */
	public float evaluate(String expression, Well well) throws CalculationException {
		Object result = executeJEP(expression, well);
		if (result instanceof Number) {
			return ((Number)result).floatValue();
		} else {
			return Float.NaN;
		}
	}

	/**
	 * Evaluate an expression for a single well.
	 * The result value is expected to be an array of numeric values.
	 * This is used mainly for expressions that contain subwell features.
	 */
	public float[] evaluateArray(String expression, Well well) throws CalculationException {
		Object result = executeJEP(expression, well);

		float[] values;
		if (result instanceof Scaler) {
			values = new float[1];
			values[0] = ((Scaler)result).floatValue();
		}
		else if (result instanceof MVector) {
			MVector vector = (MVector) result;
			values = new float[vector.getNumEles()];
			for (int i=0; i<vector.getNumEles(); i++){
				if (vector.getEle(i) instanceof Complex) {
					values[i] = Float.NaN;
				} else {
					values[i] = ((Number)vector.getEle(i)).floatValue();
				}
			}
		}
		else {
			try {
				values = new float[1];
				values[0] = (((Number)result).floatValue());
			} catch (Exception e) {
				String resultClass = (result == null) ? "<null>" : result.getClass().getSimpleName();
				throw new CalculationException("Cannot convert value to numeric array: " + resultClass);
			}
		}
		return values;
	}

	/**
	 * Evaluate an expression for a list of wells.
	 * Each well is expected to return a single value.
	 * The resulting array thus has a length equal to the number of wells.
	 */
	public float[] evaluateArray(String expression, List<Well> wells) throws CalculationException {
		float[] results = new float[wells.size()];
		for (int i = 0; i < wells.size(); i++) {
			results[i] = evaluate(expression, wells.get(i));
		}
		return results;
	}

	/**
	 * Evaluate an expression for an arbitrary object.
	 * If the object is not a Well, a jepScanner extension should be available that can
	 * interpret the object (see {@link IScanner}). 
	 */
	public Object evaluateArray(String expression, Object object) throws CalculationException {
		Object result = executeJEP(expression, object);

		if (result instanceof Scaler) {
			return new float[] { ((Scaler) result).floatValue() };
		} else if (result instanceof MVector) {
			MVector vector = (MVector) result;
			int numEles = vector.getNumEles();
			boolean isNumeric = true;
			if (numEles > 0) {
				isNumeric = !(vector.getEle(0) instanceof String);
			}

			if (isNumeric) {
				float[] values = new float[numEles];
				for (int i = 0; i < numEles; i++){
					if (vector.getEle(i) instanceof Complex) {
						values[i] = ((Complex)vector.getEle(i)).floatValue();
					} else {
						values[i] = ((Number)vector.getEle(i)).floatValue();
					}
				}
				return values;
			} else {
				String[] values = new String[numEles];
				for (int i = 0; i < numEles; i++){
					values[i] = vector.getEle(i).toString();
				}
				return values;
			}
		} else {
			try {
				return new float[] { (((Number)result).floatValue()) };
			} catch (Exception e) {
				String resultClass = (result == null) ? "<null>" : result.getClass().getSimpleName();
				throw new CalculationException("Cannot convert value to numeric array: " + resultClass);
			}
		}
	}

	/*
	 * Non-public
	 * **********
	 */

	private Object executeJEP(String expr, Object object) {
		Object retVal = null;
		try {
			JEP jep = JEPParser.parse(expr, object);
			Node node = jep.getTopNode();
			retVal = jep.evaluate(node);
		} catch (CalculationException e) {
			throw e;
		} catch (ParseException e) {
			throw new CalculationException("JEP Evaluation failed: " + e.getMessage(), e);
		}
		return retVal;
	}

}