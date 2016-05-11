package eu.openanalytics.phaedra.base.event;

public class ModelEvent {

	public Object source;
	
	public ModelEventType type;
	
	public int status;
	
	public ModelEvent() {
		// Default constructor.
	}
	
	public ModelEvent(Object source, ModelEventType type, int status) {
		this.source = source;
		this.type = type;
		this.status = status;
	}
}
