package eu.openanalytics.phaedra.base.scripting.jep.functions;

import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

public class Offset extends PostfixMathCommand implements CallbackEvaluationI {

	public Offset() {
		super();
		this.numberOfParameters = -1;
	}
	
	@Override
	public Object evaluate(Node node, EvaluatorI pv) throws ParseException {
		Object condVal = pv.eval(node.jjtGetChild(0));
		// Get value of argument
		double offsetValue = (Double) pv.eval(node.jjtGetChild(1));

		if (condVal instanceof MatrixValueI) {
			MatrixValueI condValues = (MatrixValueI) condVal;
			MVector results = new MVector(condValues.getNumEles());

			for (int i = 0; i < condValues.getNumEles(); i++) {
				results.setEle(i, ((Number) condValues.getEle(i)).doubleValue() + offsetValue);
			}

			return results;
		} else if (condVal instanceof Number) {
			return ((Number) condVal).doubleValue() + offsetValue;
		} else {
			return condVal;
		}
	}

}