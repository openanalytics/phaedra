package eu.openanalytics.phaedra.base.ui.charting.v2.chart;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TooltipsSettings implements Serializable {

	private static final long serialVersionUID = -4535723127011155705L;

	private boolean showLabels;
	private boolean showCoords;
	private int fontSize;

	// Misc settings.
	private Map<String, Object> miscSettings;

	public TooltipsSettings() {
		this.fontSize = 11;
		this.miscSettings = new HashMap<>();
	}

	public boolean isShowLabels() {
		return showLabels;
	}

	public void setShowLabels(boolean showLabels) {
		this.showLabels = showLabels;
	}

	public boolean isShowCoords() {
		return showCoords;
	}

	public void setShowCoords(boolean showCoords) {
		this.showCoords = showCoords;
	}

	public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	public Object getMiscSetting(String key) {
		if (miscSettings == null) miscSettings = new HashMap<>();
		return miscSettings.get(key);
	}

	public void setMiscSetting(String key, Object setting) {
		if (miscSettings == null) miscSettings = new HashMap<>();
		miscSettings.put(key, setting);
	}

}