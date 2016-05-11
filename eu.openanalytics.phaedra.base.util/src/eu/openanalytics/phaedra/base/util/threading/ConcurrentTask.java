package eu.openanalytics.phaedra.base.util.threading;

public class ConcurrentTask {

	private Object result;
	private Exception exception;
	
	public void run() {
		// Default: do nothing.
	}
	
	public Object getResult() {
		return result;
	}
	
	public void setResult(Object result) {
		this.result = result;
	}
	
	public void setException(Exception exception) {
		this.exception = exception;
	}
	
	public Exception getException() {
		return exception;
	}
}
