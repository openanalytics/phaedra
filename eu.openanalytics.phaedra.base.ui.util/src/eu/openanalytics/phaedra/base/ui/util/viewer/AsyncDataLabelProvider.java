package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.function.Supplier;

import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.misc.DataLoadStatus;


/**
 * Label provider for async loaded data.
 * 
 * @param <TEntity> the type of elements in the base input and data loader
 * @param <TViewerElement> the type of elements in the viewer
 * @param <TData> the type of the loaded data
 * 
 * @see AsyncDataDirectLabelProvider
 */
public abstract class AsyncDataLabelProvider<TEntity, TViewerElement, TData> extends DataFormatLabelProvider {
	
	
	protected final AsyncDataLoader<TEntity>.DataAccessor<TData> dataAccessor;
	
	
	public AsyncDataLabelProvider(final AsyncDataLoader<TEntity>.DataAccessor<TData> dataAccessor,
			final DataDescription dataDescription, final Supplier<DataFormatter> dataFormatSupport) {
		super(dataDescription, dataFormatSupport);
		this.dataAccessor = dataAccessor;
	}
	
	public AsyncDataLabelProvider(final AsyncDataLoader<TEntity>.DataAccessor<TData> dataAccessor) {
		super(null, null);
		this.dataAccessor = dataAccessor;
	}
	
	
	protected abstract Object getData(final TViewerElement element);
	
	@Override
	public void update(final ViewerCell cell) {
		final TViewerElement element = (TViewerElement)cell.getElement();
		final Object r = getData(element);
		if (r instanceof DataLoadStatus) {
			cell.setText(r.toString());
			cell.setImage(null);
			cell.setBackground(null);
			cell.setForeground(null);
			cell.setFont(null);
		}
		else {
			final TData data = (TData)r;
			cell.setText(getText(element, data));
			cell.setImage(getImage(element, data));
			cell.setBackground(getBackground(element, data));
			cell.setForeground(getForeground(element, data));
			cell.setFont(null);
		}
	}
	
	@Override
	public String getText(final Object obj) {
		final TViewerElement element = (TViewerElement)obj;
		final Object r = getData(element);
		if (r instanceof DataLoadStatus) {
			return r.toString();
		}
		return getText(element, (TData)r);
	}
	
	@Override
	public Image getImage(final Object element) {
		return null;
	}
	
	
	protected String getText(final TViewerElement element, final TData data) {
		return super.getText(data);
	}
	
	protected Image getImage(final TViewerElement element, final TData data) {
		return null;
	}
	
	protected Color getBackground(final TViewerElement element, final TData data) {
		return null;
	}
	
	protected Color getForeground(final TViewerElement element, final TData data) {
		return null;
	}
	
}
