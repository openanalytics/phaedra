package eu.openanalytics.phaedra.base.scripting.jep.functions;

import java.util.Stack;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.function.UnaryOperatorI;
import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.lsmp.djep.vectorJep.values.Scaler;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.Add;
import org.nfunk.jep.function.PostfixMathCommand;

public class Glog extends PostfixMathCommand implements UnaryOperatorI{

	Add add = new Add();
	double percent = 5d;

	public Glog()
	{
		super();
		this.numberOfParameters = -1;
	}

	@Override
	public Dimensions calcDim(Dimensions arg0) {
		return Dimensions.ONE;
	}

	@Override
	public MatrixValueI calcValue(MatrixValueI res, MatrixValueI lhs) throws ParseException {

		if (!(res instanceof MVector)) {
			throw new ParseException("Glog: result must be a list");
		}

		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (int i = 0; i < lhs.getNumEles(); i++) stats.addValue((double) lhs.getEle(i));

		// Use the 5th percentile as advised by "Alternatives to Log-Scale Data Display" article
		double lambda = stats.getPercentile(percent);
		
		for(int i = 0; i < res.getNumEles(); i++) {
			res.setEle(i, Math.log((double) lhs.getEle(i) + Math.sqrt(Math.pow((double) lhs.getEle(i), 2) + lambda)));
		}

		return res;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void run(Stack s) throws ParseException
	{
		checkStack(s);// check the stack
		if (curNumberOfParameters < 1) throw new ParseException("No arguments for Glog");

		Stack<Double> values = new Stack<>();

		if (s.size() > 1) {
			Object perc = s.pop();
			if (perc instanceof Number) {
				percent = (double) perc;
			}
		}
		
		Object obj = s.pop();
		
		if (obj instanceof Scaler) {
			values.add(((Scaler)obj).doubleValue()); 
		} else if (obj instanceof MVector) {
			MVector vector = (MVector) obj;

			for(int i = 0; i < vector.getNumEles(); i++){
				values.add(((Number)vector.getEle(i)).doubleValue());
			}
		} else if(obj instanceof Number) {
			values.add(((Number)obj).doubleValue());
		} else {
			throw new ParseException("Glog: Can't handle values");
		}

		MVector valuesVector = new MVector(values.size());
		for(int i = 0; i < values.size();i++){
			valuesVector.setEle(i, values.get(i));
		}

		MVector res = new MVector(values.size());
		calcValue(res, valuesVector);
		s.push(res);
	}

}
