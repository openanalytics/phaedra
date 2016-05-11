package eu.openanalytics.phaedra.ui.protocol.event;

public class UIEvent {

	public static enum EventType {
		FeatureSelectionChanged,
		NormalizationSelectionChanged,
		FeatureGroupSelectionChanged,
		ImageSettingsChanged, 
		ColorMethodChanged
	}
	
	public EventType type;
	
	public UIEvent(EventType type) {
		this.type = type;
	}
}
