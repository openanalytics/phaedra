package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import java.lang.reflect.Array;
import java.util.function.Supplier;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.datatype.util.DataTypeGuesser;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncData1toNViewerInput;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataLabelProvider;
import eu.openanalytics.phaedra.base.ui.util.viewer.AsyncDataViewerInput;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.DynamicColumns;


public class DynamicColumnLabelProvider<TEntity, TViewerElement> extends AsyncDataLabelProvider<TEntity, TViewerElement, Object> {
	
	
	private DataProvider<TEntity, TViewerElement> dataProvider;
	
	private final AsyncDataViewerInput<TEntity, TViewerElement> viewerInput;
	
	private final DataTypeGuesser dataTypeGuesser;
	
	
	public DynamicColumnLabelProvider(final DataDescription dataDescription,
			final DataProvider<TEntity, TViewerElement> dataProvider,
			final Supplier<DataFormatter> dataFormatSupplier) {
		super(dataProvider.getDataAccessor(), dataDescription, dataFormatSupplier);
		this.dataProvider = dataProvider;
		this.viewerInput = dataProvider.getViewerInput();
		this.dataTypeGuesser = (dataDescription != null) ?
				null :
				new DataTypeGuesser("", this.viewerInput.getViewerElementType());
	}
	
	@Override
	public void dispose() {
		if (this.dataProvider != null) {
			this.dataProvider.dispose();
			this.dataProvider = null;
		}
		super.dispose();
	}
	
	
	public DataProvider<TEntity, TViewerElement> getDataProvider() {
		return this.dataProvider;
	}
	
	public DataProvider<TEntity, TViewerElement> disconnectDataProvider() {
		final DataProvider<TEntity, TViewerElement> dataProvider = this.dataProvider;
		this.dataProvider = null;
		return dataProvider;
	}
	
	
	@Override
	protected Object getData(final TViewerElement element) {
		final TEntity baseElement = this.viewerInput.getBaseElement(element);
		final Object data = this.dataAccessor.getData(baseElement);
		if (data == null) {
			return null;
		}
		if (this.viewerInput instanceof AsyncData1toNViewerInput && data.getClass().isArray()) {
			final AsyncData1toNViewerInput<TEntity, TViewerElement> mappedInput = (AsyncData1toNViewerInput<TEntity, TViewerElement>)this.viewerInput;
			final int idx = mappedInput.getViewerElementIndexOf(baseElement, element);
			if (idx != -1) {
				return Array.get(data, idx);
			}
			return DynamicColumns.INVALID_VALUE_STATUS;
		}
		return data;
	}
	
	
	@Override
	protected DataDescription getDataDescription(final Object value) {
		final DataDescription dataDescription = super.getDataDescription(value);
		return (dataDescription != null) ?
				dataDescription :
				this.dataTypeGuesser.guessDataDescription(value);
	}
	
}
