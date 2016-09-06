package eu.openanalytics.phaedra.base.scripting.jep.functions;

import java.util.Stack;

import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.function.BinaryOperatorI;
import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.lsmp.djep.vectorJep.values.Tensor;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.Comparative;

/**
 * Implements the comperative functions for vectors also alows vectors to be compared with single values
 * 
 * @author Rich Morris
 * Created on 10-Dec-2004
 */
public class CostumComparative extends Comparative implements BinaryOperatorI {

	public CostumComparative(int index) {super(index);}
	public Dimensions calcDim(Dimensions ldim,Dimensions rdim)
	{
		if(ldim.equals(rdim)) return ldim;
		return null;
	}

	/**
	 * Compare the inputs element by element putting the results in res.
	 */
	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI lhs,MatrixValueI rhs)
		throws ParseException {

			int len = res.getNumEles();
			for(int i=0;i<len;++i)
			{
				boolean val=false;
				switch(id)
				{
				case LT: val = lt(lhs.getEle(i),rhs.getEle(i)); break;
				case GT: val = gt(lhs.getEle(i),rhs.getEle(i)); break;
				case LE: val = le(lhs.getEle(i),rhs.getEle(i)); break;
				case GE: val = ge(lhs.getEle(i),rhs.getEle(i)); break;
				case NE: val = ne(lhs.getEle(i),rhs.getEle(i)); break;
				case EQ: val = eq(lhs.getEle(i),rhs.getEle(i)); break;
				}
				res.setEle(i,val?new Double(1):new Double(0));
			}
			return res;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack) throws ParseException {
		Object rhsObj = inStack.pop(); 
		Object lhsObj = inStack.pop();

		if (lhsObj instanceof MatrixValueI || rhsObj instanceof MatrixValueI) {
			if (lhsObj instanceof Number) {
				lhsObj = createVector(((MatrixValueI) rhsObj).getNumEles(), ((Number)lhsObj).doubleValue());
			} else if (lhsObj instanceof String) {
				lhsObj = createVector(((MatrixValueI) rhsObj).getNumEles(), (String)lhsObj);
			}
			MatrixValueI lhs = (MatrixValueI) lhsObj;
			if (rhsObj instanceof Number) {
				rhsObj = createVector(((MatrixValueI) lhsObj).getNumEles(), ((Number)rhsObj).doubleValue());
			} else if (rhsObj instanceof String) {
				rhsObj = createVector(((MatrixValueI) lhsObj).getNumEles(), (String)rhsObj);
			}
			MatrixValueI rhs = (MatrixValueI) rhsObj;
			
			if (!lhs.getDim().equals(rhs.getDim())) {
				throw new ParseException("ElementComparative: dimensions of both sides must be equal");
			}
			
			Dimensions dims = this.calcDim(lhs.getDim(),lhs.getDim());
			MatrixValueI res = Tensor.getInstance(dims);
			calcValue(res,lhs,rhs);
			inStack.push(res);
		} else {
			//use comperative if none of the values are a vector
			inStack.push(lhsObj);
			inStack.push(rhsObj);
			super.run(inStack);
		}
	}

	//Create vector of single value
	private MatrixValueI createVector(int size, double value) {
		MVector vector = new MVector(size);
		for (int i = 0; i < size; i++) {
			vector.setEle(i, value);
		}
		return vector;
	}
	
	private MatrixValueI createVector(int size, String value) {
		MVector vector = new MVector(size);
		for (int i = 0; i < size; i++) {
			vector.setEle(i, value);
		}
		return vector;
	}
}
