package eu.openanalytics.phaedra.base.ui.util.viewer;

import java.util.function.Supplier;

import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.model.EntityProperty;
import eu.openanalytics.phaedra.base.ui.util.viewer.DataFormatLabelProvider;


public class PropertyColumnLabelProvider<T> extends DataFormatLabelProvider {
	
	
	private final EntityProperty<T> property;
	
	
	public PropertyColumnLabelProvider(final EntityProperty<T> property, final Supplier<DataFormatter> dataFormatSupplier) {
		super(property.getDataDescription(), dataFormatSupplier);
		this.property = property;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public String getText(final Object element) {
		return super.getText(this.property.getTypedValue((T)element));
	}
	
}
