package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import org.eclipse.jface.viewers.ColumnLabelProvider;


public class DynamicErrorLabelProvider<TEntity, TViewerElement> extends ColumnLabelProvider {
	
	
	private final String message;
	
	
	public DynamicErrorLabelProvider(final String message) {
		this.message = message;
	}
	
	
	@Override
	public String getText(final Object element) {
		return super.getText(this.message);
	}
	
}
