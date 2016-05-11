package eu.openanalytics.phaedra.base.ui.charting.util;

public class GraphSettingEntity {
	private String settingName;
	private boolean selected;
	public String getSettingName() {
		return settingName;
	}
	public void setSettingName(String settingName) {
		this.settingName = settingName;
	}
	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
