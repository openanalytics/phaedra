package eu.openanalytics.phaedra.protocol.template;

public class ProtocolTemplateException extends Exception {

	private static final long serialVersionUID = 1235316699352153752L;

	public ProtocolTemplateException(String msg) {
		super(msg);
	}
	
	public ProtocolTemplateException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
