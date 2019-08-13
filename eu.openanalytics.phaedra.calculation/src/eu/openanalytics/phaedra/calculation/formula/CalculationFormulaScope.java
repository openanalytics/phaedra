package eu.openanalytics.phaedra.calculation.formula;

public enum CalculationFormulaScope {

	PerWell,
	PerPlate;
	
	public int getCode() {
		return 0;
	}
	
	public String getDescription() {
		return null;
	}
	
	public static CalculationFormulaScope getForCode(int code) {
		return PerWell;
	}
	
}
