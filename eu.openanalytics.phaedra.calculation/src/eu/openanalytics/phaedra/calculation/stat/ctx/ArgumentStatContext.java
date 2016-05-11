package eu.openanalytics.phaedra.calculation.stat.ctx;

public class ArgumentStatContext extends SimpleStatContext {

	private Object[] args;
	
	public ArgumentStatContext(double[] data) {
		super(data);
	}
	
	public ArgumentStatContext(double[] data, Object[] args) {
		super(data);
		this.args = args;
	}
	
	public ArgumentStatContext(double[][] data) {
		super(data);
	}
	
	public ArgumentStatContext(double[][] data, Object[] args) {
		super(data);
		this.args = args;
	}
	
	public void setArgs(Object[] args) {
		this.args = args;
	}
	
	public Object[] getArgs() {
		return args;
	}
	
	public Object getArg(int i) {
		return args[i];
	}
}
