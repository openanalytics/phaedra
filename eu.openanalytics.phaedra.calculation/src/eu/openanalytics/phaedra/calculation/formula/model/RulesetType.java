package eu.openanalytics.phaedra.calculation.formula.model;

public enum RulesetType {

	HitCalling(0, "Hit Calling"),
	OutlierDetection(1, "Outlier Detection");
	
	private int code;
	private String label;

	private RulesetType(int code, String label) {
		this.code = code;
		this.label = label;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getLabel() {
		return label;
	}
	
	public static RulesetType get(int code) {
		for (RulesetType t: values()) {
			if (t.code == code) return t;
		}
		return null;
	}
	
}
