package eu.openanalytics.phaedra.calculation.formula.model;

import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public enum InputType {

	RawValue(0, "Raw values", "Evaluates the formula on the raw feature values"),
	NormalizedValue(1, "Normalized values", "Evaluates the formula on the normalized feature values");
	
	private int code;
	private String label;
	private String description;

	private InputType(int code, String label, String description) {
		this.code = code;
		this.label = label;
		this.description = description;
	}
	
	public int getCode() {
		return code;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getDescription() {
		return description;
	}
	
	public double getInputValue(Well well, Feature feature) {
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
		switch(this) {
		case RawValue:
			return accessor.getNumericValue(well, feature, null);
		case NormalizedValue:
			return accessor.getNumericValue(well, feature, feature.getNormalization());
		default:
			return Double.NaN;
		}
	}
	
	public static InputType get(int code) {
		return RawValue;
	}
}
