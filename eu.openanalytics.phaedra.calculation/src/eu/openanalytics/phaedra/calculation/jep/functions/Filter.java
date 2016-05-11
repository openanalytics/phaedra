package eu.openanalytics.phaedra.calculation.jep.functions;
import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.nfunk.jep.EvaluatorI;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.CallbackEvaluationI;
import org.nfunk.jep.function.PostfixMathCommand;
import org.nfunk.jep.type.Complex;
/**Info From original if function in Jep
 * The if(condExpr,posExpr,negExpr) function.
 * The value of trueExpr will be returned if condExpr is &gt;0 or Boolean.TRUE
 * and value of negExpr will be returned if condExpr is &lt;= 0 or Boolean.TRUE.
 * <p>
 * This function performs lazy evaluation so that
 * only posExpr or negExpr will be evaluated.
 * For Complex numbers only the real part is used.
 * <p>
 * An alternate form if(condExpr,posExpr,negExpr,zeroExpr)
 * is also available. Note most computations
 * are carried out over floating point doubles so
 * testing for zero can be dangerous.
 * <p>
 * This function implements the SpecialEvaluationI interface
 * so that it handles setting the value of a variable. 
 * @author Rich Morris
 * Created on 18-Nov-2003
 * @version 2.3.0 beta 1 now supports a Boolean first argument.
 * @since Feb 05 Handles Number arguments, so works with Integers rather than just Doubles
 */
public class Filter extends PostfixMathCommand implements CallbackEvaluationI {

	/**
	 * 
	 */
	public Filter() {
		super();
		numberOfParameters = 2;
	}

	/*
	 * Performs the specified action on an expression tree.
	 * Serves no function in standard JEP but 
	 * @param node top node of the tree
	 * @param pv	The visitor, can be used evaluate the children.
	 * @return top node of the results.
	 * @throws ParseException
	public Node process(Node node,Object data,ParserVisitor pv) throws ParseException
	{
		return null;
	}
    */
	/**
	 * Checks the number of parameters of the call.
	 * 
	 */
	public boolean checkNumberOfParameters(int n) {
		return (n == 2);
	}

	/**
	 * 
	 */
	public Object evaluate(Node node,EvaluatorI pv) throws ParseException
	{
		int num = node.jjtGetNumChildren(); 
		if( !checkNumberOfParameters(num))
			throw new ParseException("Filter operator must have 2 arguments.");

		// get value of argument

		Object condVal = pv.eval(node.jjtGetChild(0));
		
		
		double val;
		if(condVal instanceof Boolean)
		{
			if(((Boolean) condVal).booleanValue())
				return pv.eval(node.jjtGetChild(1));
			return Double.NaN;
		}
		else if(condVal instanceof Complex)
			val = ((Complex) condVal).re();
		else if(condVal instanceof Number)
			val = ((Number) condVal).doubleValue();
		else if (condVal instanceof MatrixValueI){
			Object values = pv.eval(node.jjtGetChild(1));
			if (!(values instanceof MatrixValueI))
				throw new ParseException("Filter values is not a vector.");
			MatrixValueI valuesMatrix = (MatrixValueI) values;
			MatrixValueI condValues = (MatrixValueI) condVal;
			if(valuesMatrix.getNumEles() != condValues.getNumEles())
				throw new ParseException("Filter conditions vector and values vector is different size.");
			int countValuesToAdd = 0;
			for (int i = 0; i < condValues.getNumEles(); i++) {
				if(((Number)condValues.getEle(i)).doubleValue()>0.0 ) countValuesToAdd++;
			}
			MVector results = new MVector(countValuesToAdd);

			int count = 0;
			for (int i = 0; i < condValues.getNumEles(); i++) {
				if(((Number)condValues.getEle(i)).doubleValue()>0.0 ){
					results.setEle(count, valuesMatrix.getEle(i));
					count++;
				}
			}
			return results;
		}
		else
			throw new ParseException("Condition in filter operator value can not be handled");

		if(val>0.0)
			return pv.eval(node.jjtGetChild(1));
		else if(num ==3 || val <0.0)
			return Double.NaN;
		return Double.NaN;
	}
}