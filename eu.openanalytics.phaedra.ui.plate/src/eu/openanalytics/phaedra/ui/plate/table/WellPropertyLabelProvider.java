package eu.openanalytics.phaedra.ui.plate.table;

import java.util.function.Supplier;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.format.ConcentrationFormat;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Well;


class WellPropertyLabelProvider extends RichLabelProvider {
	
	
	private final WellProperty wellProperty;
	private final Supplier<DataFormatter> dataFormatSupplier;
	
	
	public WellPropertyLabelProvider(final ColumnConfiguration config, final WellProperty property,
			final Supplier<DataFormatter> dataFormatSupplier) {
		super(config);
		this.wellProperty = property;
		this.dataFormatSupplier = dataFormatSupplier;
	}
	
	
	protected ConcentrationFormat createFormat() {
		return new ConcentrationFormat(DataTypePrefs.getDefaultConcentrationUnit(), DataTypePrefs.getDefaultConcentrationFormatDigits());
	}
	
	@Override
	public String getText(final Object element) {
		return this.dataFormatSupplier.get().format(
				this.wellProperty.getTypedValue((Well)element),
				this.wellProperty.getDataDescription() );
	}
	
}
