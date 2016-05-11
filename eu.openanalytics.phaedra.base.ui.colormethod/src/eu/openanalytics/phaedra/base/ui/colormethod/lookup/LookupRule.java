package eu.openanalytics.phaedra.base.ui.colormethod.lookup;

import org.eclipse.swt.graphics.RGB;

public class LookupRule {
	
	private RGB color;
	private String condition;
	private double value;

	public LookupRule(RGB color, String condition, double value) {
		this.color = color;
		this.condition = condition;
		this.value = value;
	}

	public boolean matches(double value) {
		if (condition.equals("lt")) return (value < this.value);
		if (condition.equals("gt")) return (value > this.value);
		if (condition.equals("le")) return (value <= this.value);
		if (condition.equals("ge")) return (value >= this.value);
		if (condition.equals("eq")) return (value == this.value);
		if (condition.equals("ne")) return (value != this.value);
		if (condition.equals("else")) return true;
		return false;
	}
	
	public void setColor(RGB color) {
		this.color = color;
	}

	public RGB getColor() {
		return color;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getCondition() {
		return condition;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getValue() {
		return value;
	}
}
