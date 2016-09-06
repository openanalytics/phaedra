package eu.openanalytics.phaedra.base.scripting.jep.parse;

public class VarReference {

	private String refToReplace;
	private String refName;
	private Object value;
	
	public VarReference(String refToReplace, String refName, Object value) {
		this.refToReplace = refToReplace;
		this.refName = refName;
		this.value = value;
	}
	
	public void execute(JEPExpression expression) {
		if (expression == null) return;
		if (refToReplace == null || refName == null) return;
		
		String exp = expression.getExpression();
		String modifiedExp = exp.replace(refToReplace, refName);
		expression.setExpression(modifiedExp);
		
		Object valueToSet = Double.NaN;
		if (value != null) {
			valueToSet = value;
		}
		expression.getJep().addVariable(refName, valueToSet);
	}
}
