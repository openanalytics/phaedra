package eu.openanalytics.phaedra.base.ui.util.viewer;

import org.eclipse.swt.widgets.Event;

import eu.openanalytics.phaedra.base.ui.util.misc.DataLoadStatus;


public class AsyncDataConditionalLabelProvider<TViewerElement, TData> extends ConditionalLabelProvider {
	
	
	private final AsyncDataLabelProvider<?, TViewerElement, TData> labelProvider;
	
	
	public AsyncDataConditionalLabelProvider(final AsyncDataLabelProvider<?, TViewerElement, TData> labelProvider,
			final Renderer renderer) {
		super(renderer);
		this.labelProvider = labelProvider;
	}
	
	
	protected AsyncDataLabelProvider<?, TViewerElement, TData> getLabelProvider() {
		return this.labelProvider;
	}
	
	
	@Override
	protected void measure(final Event event, final Object element) {
		measure(event, element, this.labelProvider.getText(element));
	}
	
	@Override
	protected void paint(final Event event, final Object obj) {
		final TViewerElement element = (TViewerElement)obj;
		final Object data = this.labelProvider.getData(element);
		String text;
		double value;
		if (data instanceof DataLoadStatus) {
			text = data.toString();
			value = Double.NaN;
		}
		else {
			text = this.labelProvider.getText(element, (TData)data);
			value = getNumericValue(element, (TData)data);
		}
		super.paint(event, element, text, value);
	}
	
	
	protected double getNumericValue(final TViewerElement element, final TData data) {
		if (data instanceof Number) {
			return ((Number)data).doubleValue();
		}
		else if (data instanceof String) {
			try {
				return Double.parseDouble((String)data);
			} catch (final NumberFormatException e) {
				return Double.NaN;
			}
		} else {
			return Double.NaN;
		}
	}
	
}
