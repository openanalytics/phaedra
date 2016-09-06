package eu.openanalytics.phaedra.base.scripting.jep;

import java.util.Map;

import javax.script.ScriptException;

import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.Scaler;
import org.nfunk.jep.JEP;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.type.Complex;

import eu.openanalytics.phaedra.base.console.InteractiveConsole;
import eu.openanalytics.phaedra.base.scripting.engine.BaseScriptEngine;
import eu.openanalytics.phaedra.base.scripting.jep.parse.JEPParser;

public class JEPScriptEngine extends BaseScriptEngine {

	@Override
	public void initialize() throws ScriptException {
		String label = getLabel();
		if (label == null) label = getId();
		InteractiveConsole console = new InteractiveConsole(label, null) {
			@Override
			protected String processInput(String input) throws Exception {
				Object output = executeJEP(input, null);
				if (output == null) return null;
				return output.toString();
			}
		};
		setConsole(console);
	}

	@Override
	public Object eval(String script, Map<String, Object> objects) throws ScriptException {
		Object data = objects.get("data");
		return executeJEP(script, data);
	}

	@Override
	public void registerAPI(String name, Object value, String help) {
		// Ignore, JEP does not support API additions.
	}
	
	/**
	 * Evaluate the expression against the object.
	 * 
	 * @param expr The expression to evaluate
	 * @param object The object to evaluate the expression against
	 * @return Any of the following: float, float[], String, String[]
	 * @throws ScriptException If the evaluation fails for any reason
	 */
	private Object executeJEP(String expr, Object object) throws ScriptException {
		Object retVal = null;
		try {
			JEP jep = JEPParser.parse(expr, object);
			Node node = jep.getTopNode();
			retVal = jep.evaluate(node);
		} catch (ParseException e) {
			throw new ScriptException("JEP Evaluation failed: " + e.getMessage());
		}
		
		if (retVal instanceof Number) {
			return ((Number)retVal).floatValue();
		} else if (retVal instanceof Scaler) {
			return ((Scaler)retVal).floatValue();
		} else if (retVal instanceof MVector) {
			MVector vector = (MVector) retVal;
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
		} else if (retVal instanceof String || retVal instanceof String[]) {
			return retVal;
		} else {
			return Float.NaN;
		}
	}
	
}
