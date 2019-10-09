package eu.openanalytics.phaedra.calculation.formula.model;

import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

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
	
	public double getInputValue(Well well, IFeature feature) {
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
		switch(this) {
		case RawValue:
			return accessor.getNumericValue(well, (Feature) feature, null);
		case NormalizedValue:
			return accessor.getNumericValue(well, (Feature) feature, ((Feature) feature).getNormalization());
		default:
			return Double.NaN;
		}
	}
	
	public static InputType get(int code) {
		for (InputType t: values()) {
			if (t.code == code) return t;
		}
		return null;
	}
	
	public static InputType getByLabel(String label) {
		for (InputType t: values()) {
			if (t.label.equals(label)) return t;
		}
		return null;
	}
}
