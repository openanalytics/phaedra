package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.function.Supplier;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;


public class DataFormatLabelProvider extends ColumnLabelProvider {
	
	
	private final DataDescription dataDescription;
	
	private final Supplier<DataFormatter> dataFormatSupplier;
	
	
	public DataFormatLabelProvider(final DataDescription dataDescription,
			final Supplier<DataFormatter> dataFormatSupplier) {
		this.dataDescription = dataDescription;
		this.dataFormatSupplier = dataFormatSupplier;
	}
	
	
	protected final DataFormatter getDataFormatter() {
		return this.dataFormatSupplier.get();
	}
	
	protected DataDescription getDataDescription(final Object value) {
		return this.dataDescription;
	}
	
	
	@Override
	public String getText(final Object element) {
		if (this.dataFormatSupplier != null) {
			return this.dataFormatSupplier.get().format(element, getDataDescription(element));
		}
		return (element != null) ? element.toString() : "";
	}
	
}
