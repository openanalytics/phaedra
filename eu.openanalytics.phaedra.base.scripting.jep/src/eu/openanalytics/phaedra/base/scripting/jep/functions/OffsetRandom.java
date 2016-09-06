package eu.openanalytics.phaedra.base.scripting.jep.functions;

import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

public class OffsetRandom extends PostfixMathCommand implements CallbackEvaluationI {

	public OffsetRandom() {
		super();
		this.numberOfParameters = -1;
	}
	
	@Override
	public Object evaluate(Node node, EvaluatorI pv) throws ParseException {
		Object valuesToOffset = pv.eval(node.jjtGetChild(0));
		double offsetBase = (Double) pv.eval(node.jjtGetChild(1));
		double offsetMult = (Double) pv.eval(node.jjtGetChild(2));

		if (valuesToOffset instanceof MatrixValueI) {
			MatrixValueI valueMatrix = (MatrixValueI) valuesToOffset;
			MVector results = new MVector(valueMatrix.getNumEles());

			for (int i = 0; i < valueMatrix.getNumEles(); i++) {
				double v = ((Number) valueMatrix.getEle(i)).doubleValue();
				results.setEle(i, calculate(v, offsetBase, offsetMult));
			}

			return results;
		} else if (valuesToOffset instanceof Number) {
			double v = ((Number) valuesToOffset).doubleValue();
			return calculate(v, offsetBase, offsetMult);
		} else {
			throw new ParseException("Cannot handle value type: " + valuesToOffset.getClass().getName());
		}
	}

	private double calculate(double value, double base, double mult) {
		double rnd = base + (Math.random() * mult);
		return value + rnd;
	}
}