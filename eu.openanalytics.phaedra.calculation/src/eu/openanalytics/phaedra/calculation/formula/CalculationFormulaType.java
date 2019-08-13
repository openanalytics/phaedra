package eu.openanalytics.phaedra.calculation.formula;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public enum CalculationFormulaType {

	CalculatedWellFeature(Plate.class, double[].class),
	CalculatedSubWellFeature(Well.class, float[].class),
	OutlierDetection(null, null),
	HitCalling(null, null),
	DataPolishing(null, null),
	Normalization(null, null),
	Other(null, null);
	
	private CalculationFormulaType(Class<? extends IValueObject> inputClass, Class<?> outputClass) {
		// TODO Auto-generated constructor stub
	}
	
	public int getCode() {
		return 0;
	}
	
	public String getDescription() {
		return null;
	}
	
	public void handleReturnValue(IValueObject input, Object returnValue, double[] output, CalculationFormulaScope scope) {
		
	}
	
	public static CalculationFormulaType getForCode(int code) {
		return Other;
	}
}
