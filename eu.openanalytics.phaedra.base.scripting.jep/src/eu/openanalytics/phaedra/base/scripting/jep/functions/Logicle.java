package eu.openanalytics.phaedra.base.scripting.jep.functions;

import java.util.Stack;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.lsmp.djep.vectorJep.Dimensions;
import org.lsmp.djep.vectorJep.function.UnaryOperatorI;
import org.lsmp.djep.vectorJep.values.MVector;
import org.lsmp.djep.vectorJep.values.MatrixValueI;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import edu.stanford.facs.logicle.LogicleParameterException;

public class Logicle extends PostfixMathCommand implements UnaryOperatorI {

	private double[] param;
	
	public Logicle() {
		super();
		this.param = new double[4];
		
		this.numberOfParameters = -1;
		this.param[0] = 262144;
		this.param[1] = 1.5d;
		this.param[2] = edu.stanford.facs.logicle.Logicle.DEFAULT_DECADES;
		this.param[3] = 0d; 
	}
	
	@Override
	public Dimensions calcDim(Dimensions arg0) {
		return Dimensions.ONE;
	}

	@Override
	public MatrixValueI calcValue(MatrixValueI output, MatrixValueI intput) throws ParseException {
		
		edu.stanford.facs.logicle.Logicle logicle;
		try {
			logicle = new edu.stanford.facs.logicle.Logicle(param[0], param[1], param[2], param[3]);
		} catch (final LogicleParameterException e) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openWarning(Display.getDefault().getActiveShell()
							, "Warning!", e.getMessage() + "\n\nDefault values are used instead.");
				}
			});
			logicle = new edu.stanford.facs.logicle.Logicle(262114, 1.5d);
		}
		
		for (int i = 0; i < output.getNumEles(); i++) {
			output.setEle(i, logicle.scale((double) intput.getEle(i)));
		}

		return output;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void run(Stack s) throws ParseException {
		
		// Check the stack
		checkStack(s);
		if (curNumberOfParameters < 1) {
			throw new ParseException("No arguments for Logicle");
		}

		Stack<Double> values = new Stack<>();

		for (int i = s.size(); i > 1; i--) {
			Object scaleTop = s.pop();
			if (scaleTop instanceof Number) {
				param[i - 2] = (double) scaleTop;
			}
		}
		
		Object obj = s.pop();
		
		if (obj instanceof MVector) {
			MVector vector = (MVector) obj;

			for(int i = 0; i < vector.getNumEles(); i++){
				values.add(((Number)vector.getEle(i)).doubleValue());
			}
		} else if(obj instanceof Number) {
			values.add(((Number)obj).doubleValue());
		} else {
			throw new ParseException("Logicle: Can't handle values");
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