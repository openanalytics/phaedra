package eu.openanalytics.phaedra.calculation.jep.functions;

import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

public class Drop extends PostfixMathCommand implements CallbackEvaluationI {

	public Drop() {
		super();
		this.numberOfParameters = -1;
	}
	
	@Override
	public Object evaluate(Node node, EvaluatorI pv) throws ParseException {
		Object condVal = pv.eval(node.jjtGetChild(0));
		int drop = ((Double) pv.eval(node.jjtGetChild(1))).intValue();

		if (condVal instanceof MatrixValueI) {
			MVector results = null;
			MatrixValueI condValues = (MatrixValueI) condVal;
			int newSize = condValues.getNumEles() - Math.abs(drop);
			results = new MVector(newSize);

			if (drop >= 0) {
				for (int i = drop; i < condValues.getNumEles(); i++) {
					results.setEle(i - drop, condValues.getEle(i));
				}
			} else {
				for (int i = 0; i < newSize; i++) {
					results.setEle(i, condValues.getEle(i));
				}
			}

			return results;
		} else {
			return condVal;
		}
	}

}