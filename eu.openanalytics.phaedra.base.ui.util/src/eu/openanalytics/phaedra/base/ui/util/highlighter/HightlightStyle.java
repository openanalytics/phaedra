package eu.openanalytics.phaedra.base.ui.util.highlighter;

public enum HightlightStyle {

	FLASH(true, "Flashing")
	, ROTATING(true, "Rotating")
	, STATIC(false, "Static")
	;

	private boolean running;
	private String name;

	private HightlightStyle(boolean running, String name) {
		this.running = running;
		this.name = name;
	}

	public boolean isRunning() {
		return running;
	}

	public String getName() {
		return name;
	}

	public static HightlightStyle getStyle(String name) {
		for (HightlightStyle style : HightlightStyle.values()) {
			if (style.getName().equals(name)) return style;
		}
		return ROTATING;
	}

}