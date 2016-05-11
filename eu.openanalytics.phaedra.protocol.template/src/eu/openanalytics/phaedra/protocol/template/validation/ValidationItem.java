package eu.openanalytics.phaedra.protocol.template.validation;

public class ValidationItem {
	
	public final static int SEV_INFO = 0;
	public final static int SEV_WARNING = 0x01;
	public final static int SEV_ERROR = 0x02;
	
	public int start;
	public int end;
	public int severity;
	public String text;
	
	public ValidationItem(int start, int end, int severity, String text) {
		this.start = start;
		this.end = end;
		this.severity = severity;
		this.text = text;
	}
}