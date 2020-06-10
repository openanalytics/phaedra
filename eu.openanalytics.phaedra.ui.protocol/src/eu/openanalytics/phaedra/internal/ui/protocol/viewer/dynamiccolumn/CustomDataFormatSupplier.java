package eu.openanalytics.phaedra.internal.ui.protocol.viewer.dynamiccolumn;

import java.util.function.Supplier;

import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn.ValueFormat;


public class CustomDataFormatSupplier implements Supplier<DataFormatter> {
	
	
	private final ValueFormat.FormatConfig formatConfig;
	
	private final Supplier<DataFormatter> baseSupplier;
	
	private DataFormatter currentBaseFormatter;
	private DataFormatter currentFormatter;
	
	
	public CustomDataFormatSupplier(final ValueFormat.FormatConfig formatConfig,
			final Supplier<DataFormatter> baseSupplier) {
		this.formatConfig = formatConfig;
		this.baseSupplier = baseSupplier;
	}
	
	
	@Override
	public DataFormatter get() {
		final DataFormatter baseDataFormatter = this.baseSupplier.get();
		if (this.currentBaseFormatter != baseDataFormatter) {
			this.currentBaseFormatter = baseDataFormatter;
			this.currentFormatter = this.formatConfig.getType().createDataFormatter(
					this.formatConfig, baseDataFormatter );
		}
		return this.currentFormatter;
	}
	
}
