package eu.openanalytics.phaedra.calculation.jep.functions;

import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

public class Shift extends PostfixMathCommand implements CallbackEvaluationI {

	public Shift() {
		super();
		this.numberOfParameters = -1;
	}

	@Override
	public Object evaluate(Node node, EvaluatorI pv) throws ParseException {
		// get value of argument
		int positionsToShift = ((Double) pv.eval(node.jjtGetChild(1))).intValue();
		Object condVal = pv.eval(node.jjtGetChild(0));

		if (condVal instanceof MatrixValueI) {
			MVector results = null;
			MatrixValueI condValues = (MatrixValueI) condVal;
			results = new MVector(condValues.getNumEles());

			if (positionsToShift >= 0) {
				for (int i = 0; i < positionsToShift; i++) {
					results.setEle(i, Float.NaN);
				}
				for (int i = positionsToShift; i < condValues.getNumEles(); i++) {
					results.setEle(i, condValues.getEle(i - positionsToShift));
				}
			} else {
				for (int i = 0; i < condValues.getNumEles(); i++) {
					results.setEle(i, condValues.getEle(i - positionsToShift));
				}
				for (int i = 0; i > positionsToShift; i--) {
					results.setEle(condValues.getNumEles() - 1 + i, Float.NaN);
				}
			}

			return results;
		} else {
			return condVal;
		}
	}
	
}