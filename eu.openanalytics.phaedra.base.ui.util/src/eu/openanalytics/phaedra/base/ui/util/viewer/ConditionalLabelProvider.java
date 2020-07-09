package eu.openanalytics.phaedra.base.ui.util.viewer;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;


/**
 * Label provider for conditional cell rendering.
 */
public class ConditionalLabelProvider extends OwnerDrawLabelProvider {
	
	
	public static interface Renderer {
		
		
		void paint(final GC gc, final Rectangle cellBounds,
				final String text, final double value);
		
		void dispose();
		
	}
	
	
	private final ILabelProvider labelProvider;
	private final NumericValueProvider numericValueProvider;
	
	private final Renderer renderer;
	
	
	public ConditionalLabelProvider(final ILabelProvider labelProvider, final NumericValueProvider numericValueProvider,
			final Renderer renderer) {
		this.labelProvider = labelProvider;
		this.numericValueProvider = numericValueProvider;
		this.renderer = renderer;
	}
	
	/**
	 * Override {@link #measure(Event, Object)} and {@link #paint(Event, Object)}!
	 * 
	 * @param renderer
	 */
	public ConditionalLabelProvider(final Renderer renderer) {
		this.labelProvider = null;
		this.numericValueProvider = null;
		this.renderer = renderer;
	}
	
	@Override
	public void dispose() {
		this.renderer.dispose();
		if (this.labelProvider != null) {
			this.labelProvider.dispose();
		}
		super.dispose();
	}
	
	
	private Rectangle getCellBounds(final Event event, final Object element) {
		if (event.item instanceof TableItem) {
			return ((TableItem)event.item).getBounds(event.index);
		}
		if (event.item instanceof TreeItem) {
			return ((TreeItem)event.item).getBounds(event.index);
		}
		throw new UnsupportedOperationException("item= " + event.item);
	}
	
	@Override
	protected void measure(final Event event, final Object element) {
		measure(event, element, this.labelProvider.getText(element));
	}
	
	protected void measure(final Event event, final Object element,
			final String text) {
		final GC gc = event.gc;
		
		final int textWidth = gc.stringExtent(text).x + 10;
		event.width = Math.max(textWidth, 40);
	}
	
	@Override
	protected void erase(final Event event, final Object element) {
		event.detail &= ~SWT.FOREGROUND;
	}
	
	@Override
	protected void paint(final Event event, final Object element) {
		paint(event, element,
				this.labelProvider.getText(element),
				this.numericValueProvider.getNumericValue(element) );
	}
	
	protected void paint(final Event event, final Object element,
			final String text, double value) {
		if (Double.isNaN(value) || value < 0) {
			value = 0;
		}
		if (value > 1) {
			value = 1;
		}
		
		final Rectangle cellBounds = getCellBounds(event, element);
		
		final GC gc = event.gc;
		final Color oldForeground = gc.getForeground();
		final Color oldBackground = gc.getBackground();
		this.renderer.paint(gc, cellBounds, text, value);
		gc.setForeground(oldForeground);
		gc.setBackground(oldBackground);
	}
	
	
	public static class ProgressBarRenderer implements Renderer {
		
		/** text color */
		private final Color foregroundColor;
		/** progress bar color */
		private final Color progressColor;
		
		
		public ProgressBarRenderer(final Color foregroundColor, final Color progressColor) {
			this.foregroundColor = foregroundColor;
			this.progressColor = progressColor;
		}
		
		public ProgressBarRenderer(final Color progressColor) {
			this(null, progressColor);
		}
		
		@Override
		public void dispose() {
		}
		
		
		@Override
		public void paint(final GC gc, final Rectangle cellBounds,
				final String text, final double value) {
			final int fillWidth = (int)(cellBounds.width * value);
			gc.setBackground((this.progressColor != null) ?
					this.progressColor : gc.getDevice().getSystemColor(SWT.COLOR_DARK_CYAN) );
			gc.fillRectangle(cellBounds.x, cellBounds.y + 1, fillWidth, cellBounds.height - 2);
			
			gc.setForeground((this.foregroundColor != null) ?
					this.foregroundColor : gc.getDevice().getSystemColor(SWT.COLOR_BLACK) );
			final int textHeight = gc.getFontMetrics().getHeight();
			gc.drawString(text, cellBounds.x + 4, cellBounds.y + Math.max(0, (cellBounds.height - textHeight + 1) / 2), true);
		}
		
	}
	
	public static ConditionalLabelProvider withProgressBar(final NumericValueProvider labelProvider,
			final Color progressColor) {
		return new ConditionalLabelProvider(
				(labelProvider instanceof ILabelProvider) ? labelProvider : new LabelProvider(),
				labelProvider, new ProgressBarRenderer(progressColor) );
	}
	
}
