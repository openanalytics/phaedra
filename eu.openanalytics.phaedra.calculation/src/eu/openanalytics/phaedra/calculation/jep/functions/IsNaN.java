package eu.openanalytics.phaedra.calculation.jep.functions;

import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

public class IsNaN extends PostfixMathCommand implements CallbackEvaluationI {

	public IsNaN() {
		super();
		this.numberOfParameters = -1;
	}
	
	@Override
	public Object evaluate(Node node, EvaluatorI pv) throws ParseException {
		Object condVal = pv.eval(node.jjtGetChild(0));

		if (condVal instanceof MatrixValueI) {
			MatrixValueI condValues = (MatrixValueI) condVal;
			MVector results = new MVector(condValues.getNumEles());
			for (int i = 0; i < condValues.getNumEles(); i++) {
				results.setEle(i, evaluateNumber((Number)condValues.getEle(i)));
			}
			return results;
		} else if (condVal instanceof Number) {
			return evaluateNumber((Number)condVal);
		} else {
			return condVal;
		}
	}

	private Number evaluateNumber(Number number) {
		return Double.isNaN(number.doubleValue()) ? 1.0 : 0.0;
	}
}