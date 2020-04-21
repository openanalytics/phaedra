package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.function.Function;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.util.DataValueConverter;
import eu.openanalytics.phaedra.base.ui.util.misc.AsyncDataLoader;
import eu.openanalytics.phaedra.base.ui.util.misc.DataLoadStatus;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncData1toNViewerInput;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumns;


public abstract class DataProvider<TEntity, TViewerElement> implements Function<TEntity, Object> {
	
	
	private final static DataValueConverter DATA_VALUE_CONVERTER = new DataValueConverter(DynamicColumns.INVALID_VALUE_STATUS);
	
	
	private final DataDescription dataDescription;
	
	private final AsyncDataViewerInput<TEntity, TViewerElement> viewerInput;
	private AsyncDataLoader<TEntity>.DataAccessor<Object> dataAccessor;
	
	
	public DataProvider(final DataDescription dataDescription,
			final AsyncDataViewerInput<TEntity, TViewerElement> viewerInput) {
		this.dataDescription = dataDescription;
		this.viewerInput = viewerInput;
	}
	
	public void dispose() {
		final AsyncDataLoader<TEntity>.DataAccessor<Object> dataAccessor = this.dataAccessor;
		if (dataAccessor != null) {
			this.dataAccessor.dispose();
		}
	}
	
	
	protected DataDescription getDataDescription() {
		return this.dataDescription;
	}
	
	protected AsyncDataViewerInput<TEntity, TViewerElement> getViewerInput() {
		return this.viewerInput;
	}
	
	public void initialize() {
		this.dataAccessor = this.viewerInput.getDataLoader().addDataRequest(this);
	}
	
	public AsyncDataLoader<TEntity>.DataAccessor<Object> getDataAccessor() {
		return this.dataAccessor;
	}
	
	
	protected Object checkData(final TEntity element, final Object data) {
		if (data == null) {
			return null;
		}
		if (data instanceof DataLoadStatus) {
			return data;
		}
		if (this.viewerInput instanceof AsyncData1toNViewerInput && data.getClass().isArray()) {
			final AsyncData1toNViewerInput<TEntity, TViewerElement> mappedInput = (AsyncData1toNViewerInput<TEntity, TViewerElement>)this.viewerInput;
			if (mappedInput.getViewerElementsSize(element) == Array.getLength(data)) {
				if (this.dataDescription != null) {
					return DATA_VALUE_CONVERTER.convertArray(this.dataDescription, data);
				}
				return data;
			}
			return DynamicColumns.INVALID_VALUE_STATUS;
		}
		else {
			if (this.dataDescription != null) {
				return DATA_VALUE_CONVERTER.convert(this.dataDescription, data);
			}
			return data;
		}
	}
	
	
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj != null && getClass() == obj.getClass()) {
			return (Objects.equals(this.dataDescription, ((DataProvider)obj).dataDescription));
		}
		return false;
	}
	
}
