package eu.openanalytics.phaedra.calculation.jep.functions;

import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;

public class CstOp extends PostfixMathCommand implements CallbackEvaluationI {

	public CstOp() {
		super();
		this.numberOfParameters = -1;
	}

	@Override
	public Object evaluate(Node node, EvaluatorI pv) throws ParseException {
		Object p1 = pv.eval(node.jjtGetChild(0));
		Object p2 = pv.eval(node.jjtGetChild(1));
		Object p3 = pv.eval(node.jjtGetChild(2));

		String sign = (String) p3;

		switch (sign) {
		case "+":
			if (p1 instanceof MatrixValueI) {
				MatrixValueI p1Values = (MatrixValueI) p1;
				MVector results = new MVector(p1Values.getNumEles());
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, (((Number) p1Values.getEle(i)).doubleValue() + p2Value));
					}
					return results;
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, (((Number) p1Values.getEle(i)).doubleValue() + ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			} else if (p1 instanceof Number) {
				Double p1Value = ((Number) p1).doubleValue();
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					return (p1Value + p2Value);
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					MVector results = new MVector(p2Values.getNumEles());
					for (int i = 0; i < p2Values.getNumEles(); i++) {
						results.setEle(i, (p1Value + ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			}
			break;
		case "-":
			if (p1 instanceof MatrixValueI) {
				MatrixValueI p1Values = (MatrixValueI) p1;
				MVector results = new MVector(p1Values.getNumEles());
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, (((Number) p1Values.getEle(i)).doubleValue() - p2Value));
					}
					return results;
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, (((Number) p1Values.getEle(i)).doubleValue() - ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			} else if (p1 instanceof Number) {
				Double p1Value = ((Number) p1).doubleValue();
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					return (p1Value - p2Value);
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					MVector results = new MVector(p2Values.getNumEles());
					for (int i = 0; i < p2Values.getNumEles(); i++) {
						results.setEle(i, (p1Value - ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			}
			break;
		case "*":
			if (p1 instanceof MatrixValueI) {
				MatrixValueI p1Values = (MatrixValueI) p1;
				MVector results = new MVector(p1Values.getNumEles());
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, (((Number) p1Values.getEle(i)).doubleValue() * p2Value));
					}
					return results;
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, (((Number) p1Values.getEle(i)).doubleValue() * ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			} else if (p1 instanceof Number) {
				Double p1Value = ((Number) p1).doubleValue();
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					return (p1Value * p2Value);
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					MVector results = new MVector(p2Values.getNumEles());
					for (int i = 0; i < p2Values.getNumEles(); i++) {
						results.setEle(i, (p1Value * ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			}
			break;
		case "/":
			if (p1 instanceof MatrixValueI) {
				MatrixValueI p1Values = (MatrixValueI) p1;
				MVector results = new MVector(p1Values.getNumEles());
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, (((Number) p1Values.getEle(i)).doubleValue() / p2Value));
					}
					return results;
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, (((Number) p1Values.getEle(i)).doubleValue() / ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			} else if (p1 instanceof Number) {
				Double p1Value = ((Number) p1).doubleValue();
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					return (p1Value / p2Value);
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					MVector results = new MVector(p2Values.getNumEles());
					for (int i = 0; i < p2Values.getNumEles(); i++) {
						results.setEle(i, (p1Value / ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			}
			break;
		case "^":
			if (p1 instanceof MatrixValueI) {
				MatrixValueI p1Values = (MatrixValueI) p1;
				MVector results = new MVector(p1Values.getNumEles());
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, Math.pow(((Number) p1Values.getEle(i)).doubleValue(), p2Value));
					}
					return results;
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					for (int i = 0; i < p1Values.getNumEles(); i++) {
						results.setEle(i, Math.pow(((Number) p1Values.getEle(i)).doubleValue(), ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			} else if (p1 instanceof Number) {
				Double p1Value = ((Number) p1).doubleValue();
				if (p2 instanceof Number) {
					Double p2Value = ((Number) p2).doubleValue();
					return (Math.pow(p1Value, p2Value));
				}
				if (p2 instanceof MatrixValueI) {
					MatrixValueI p2Values = (MatrixValueI) p2;
					MVector results = new MVector(p2Values.getNumEles());
					for (int i = 0; i < p2Values.getNumEles(); i++) {
						results.setEle(i, Math.pow(p1Value, ((Number) p2Values.getEle(i)).doubleValue()));
					}
					return results;
				}
			}
			break;
		}

		return null;
	}

}