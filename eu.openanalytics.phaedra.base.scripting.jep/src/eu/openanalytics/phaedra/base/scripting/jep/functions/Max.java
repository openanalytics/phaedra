package eu.openanalytics.phaedra.base.scripting.jep.functions;

import java.util.Stack;

import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.function.UnaryOperatorI;
import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.lsmp.djep.vectorJep.values.Scaler;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

public class Max extends PostfixMathCommand implements UnaryOperatorI
{

	public Max()
	{
		super();
		this.numberOfParameters = -1;
	}

	public Dimensions calcDim(Dimensions ldim)
	{
		return Dimensions.ONE;
	}

	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI lhs) throws ParseException
	{
		if(!(res instanceof Scaler))
			throw new ParseException("Max: result must be a scaler");

		Object max = Double.NaN;
		if (lhs.getNumEles() > 0) {
			max = lhs.getEle(0);
			for (int i=1;i<lhs.getNumEles();++i) {
				if (((Number)max).doubleValue() < ((Number)lhs.getEle(i)).doubleValue()) {
					max = lhs.getEle(i);
				}
			}
		}

		res.setEle(0,max);
		return res;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack s) throws ParseException
	{

		checkStack(s);// check the stack
		if (curNumberOfParameters < 1) throw new ParseException("No arguments for Max");

		Stack<Double> values = new Stack<>();

		for(int i = 0; i < curNumberOfParameters;i++){
			Object obj = s.pop();

			if(obj instanceof Scaler)
			{
				values.add(((Scaler)obj).doubleValue()); 
			}
			else if(obj instanceof MVector){
				MVector vector = (MVector) obj;

				for(int j = 0; j < vector.getNumEles(); j++){
					values.add(((Number)vector.getEle(j)).doubleValue());
				}
			}			
			else
				try{
					values.add(((Number)obj).doubleValue());
				}catch (Exception e) {
					throw new ParseException("Max: Can't handle values");
				}				
		}

		MVector valuesVector = new MVector(values.size());
		for(int i = 0; i < values.size();i++){
			valuesVector.setEle(i, values.get(i));
		}

		MatrixValueI res = Scaler.getInstance(new Double(0.0));
		calcValue(res,valuesVector);
		s.push(res);
	}
}

