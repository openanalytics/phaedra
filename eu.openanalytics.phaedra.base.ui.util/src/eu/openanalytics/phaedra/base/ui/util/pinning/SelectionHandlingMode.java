package eu.openanalytics.phaedra.base.ui.util.pinning;

public enum SelectionHandlingMode {
	SEL_HILITE("Selection + Highlighting", "pin.png")
	, SEL("Selection", "pin.png")
	, HILITE("Highlighting Only", "pin_link.png")
	, NONE("Pinned", "pin_key.png")
	;

	private String name;
	private String icon;

	private SelectionHandlingMode(String name, String icon) {
		this.name = name;
		this.icon = icon;
	}

	public String getName() {
		return name;
	}

	public String getIcon() {
		return icon;
	}

}