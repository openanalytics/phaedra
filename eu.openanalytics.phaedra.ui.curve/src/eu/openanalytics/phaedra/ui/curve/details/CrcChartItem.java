package eu.openanalytics.phaedra.ui.curve.details;

import org.jfree.data.xy.XYDataItem;

import eu.openanalytics.phaedra.model.plate.vo.Well;

public class CrcChartItem extends XYDataItem {

	private static final long serialVersionUID = -4261803055231862686L;
	
	private Well well;
	private double weight;
	
	public CrcChartItem(double x, double y) {
		super(x, y);
	}

	public Well getWell() {
		return well;
	}

	public void setWell(Well well) {
		this.well = well;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
}
