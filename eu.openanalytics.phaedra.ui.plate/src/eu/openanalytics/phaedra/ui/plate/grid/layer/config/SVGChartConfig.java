package eu.openanalytics.phaedra.ui.plate.grid.layer.config;

import java.io.Serializable;


public class SVGChartConfig implements Serializable {

	private static final long serialVersionUID = -2731355316897168335L;
	
	private String chartName;
	private boolean noBg;
	private int bgColor;
	
	public void setChartName(String chartName) {
		this.chartName = chartName;
	}
	
	public String getChartName() {
		return chartName;
	}
	
	public void setNoBg(boolean noBg) {
		this.noBg = noBg;
	}
	
	public boolean isNoBg() {
		return noBg;
	}
	
	public void setBgColor(int bgColor) {
		this.bgColor = bgColor;
	}
	
	public int getBgColor() {
		return bgColor;
	}
}
