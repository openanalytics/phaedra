package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.function.Supplier;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.model.EntityProperty;


public class PropertyColumnLabelProvider<T> extends ColumnLabelProvider {
	
	
	private final EntityProperty<T> property;
	private final Supplier<DataFormatter> dataFormatSupplier;
	
	
	public PropertyColumnLabelProvider(final EntityProperty<T> property, final Supplier<DataFormatter> dataFormatSupplier) {
		this.property = property;
		this.dataFormatSupplier = dataFormatSupplier;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public String getText(final Object element) {
		return this.dataFormatSupplier.get().format(
				this.property.getTypedValue((T)element),
				this.property.getDataDescription() );
	}
	
}
