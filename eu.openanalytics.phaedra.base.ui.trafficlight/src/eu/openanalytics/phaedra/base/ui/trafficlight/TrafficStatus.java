package eu.openanalytics.phaedra.base.ui.trafficlight;


public class TrafficStatus {

	public final static int UNKNOWN = 0;
	public final static int UP = 1;
	public final static int WARN = 2;
	public final static int DOWN = 3;
	
	private int code;
	private String message;
	private String detailedMessage;
	
	public TrafficStatus(int code, String message) {
		this(code, message, null);
	}
	
	public TrafficStatus(int code, String message, String detailedMessage) {
		this.code = code;
		this.message = message;
		if(detailedMessage != null) this.detailedMessage = detailedMessage;
		else this.detailedMessage = "Untested.";
	}
	
	public int getCode() {
		return code;
	}
	
	public String getMessage() {
		return message;
	}
	
	public String getCodeLabel() {
		switch (code) {
		case UP: return "Up";
		case WARN: return "Warn";
		case UNKNOWN: return "Unknown";
		case DOWN: return "Down";
		}
		return null;
	}
	
	public String getIconPath() {
		switch (code) {
		case UP: return "status_green.png";
		case WARN: return "status_orange.png";
		case UNKNOWN: return "status_grey.png";
		case DOWN: return "status_red.png";
		}
		return null;
	}

	public String getDetailedMessage() {
		return detailedMessage;
	}
}
