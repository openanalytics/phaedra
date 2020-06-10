package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataConditionalLabelProvider;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ConditionalFormat;


public class DynamicConditionalLabelProvider<TViewerElement, TEntity> extends AsyncDataConditionalLabelProvider<TViewerElement, Object> {
	
	
	private final ConditionalFormat.FormatConfig formatConfig;
	
	
	public DynamicConditionalLabelProvider(final DynamicColumnLabelProvider<TEntity, TViewerElement> labelProvider,
			final ConditionalFormat.FormatConfig formatConfig) {
		super(labelProvider, formatConfig.getType().createRenderer(formatConfig));
		this.formatConfig = formatConfig;
	}
	
	
	public DynamicColumnLabelProvider<TEntity, ?> getDynamicColumn() {
		return (DynamicColumnLabelProvider<TEntity, ?>)getLabelProvider();
	}
	
	
	@Override
	protected double getNumericValue(final TViewerElement element, final Object data) {
		final double value = super.getNumericValue(element, data);
		if (Double.isNaN(value)) {
			return Double.NaN;
		}
		return (value - this.formatConfig.getMin()) / (this.formatConfig.getMax() - this.formatConfig.getMin());
	}
	
}
