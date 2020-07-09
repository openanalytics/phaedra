package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.core.runtime.IStatus;


public class DataLoadStatus {
	
	public static DataLoadStatus error(final String detail) {
		if (detail == null || detail.isEmpty()) {
			return new DataLoadStatus(IStatus.ERROR, "<ERROR>");
		}
		final StringBuilder sb = new StringBuilder();
		sb.append("<ERROR: ");
		final int idx = detail.indexOf('\n');
		if (idx > 160 || (idx == -1 && detail.length() > 160)) {
			sb.append(detail.substring(0, 160));
			sb.append('\u2026');
		}
		else if (idx != -1) {
			sb.append(detail.substring(0, idx));
		}
		else {
			sb.append(detail);
		}
		return new DataLoadStatus(IStatus.ERROR, sb.toString());
	}
	
	
	private final int severity;
	
	private final String text;
	
	
	public DataLoadStatus(final int severity, final String text) {
		this.severity = severity;
		this.text = text;
	}
	
	
	public int getSeverity() {
		return this.severity;
	}
	
	public String getText() {
		return this.text;
	}
	
	
	@Override
	public String toString() {
		return this.text;
	}
	
}
