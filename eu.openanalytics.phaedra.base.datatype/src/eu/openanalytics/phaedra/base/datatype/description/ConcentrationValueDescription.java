package eu.openanalytics.phaedra.base.datatype.description;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationValueConverter;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;


public class ConcentrationValueDescription extends AbstractConcentrationDataDescription {
	
	
	public ConcentrationValueDescription(final String name, final ConcentrationUnit unit) {
		super(name, unit);
	}
	
	@Override
	public ConcentrationValueDescription alterTo(final DataUnitConfig dataUnitConfig) {
		if (dataUnitConfig.getConcentrationUnit() == getConcentrationUnit()) {
			return this;
		}
		return new ConcentrationValueDescription(getName(), dataUnitConfig.getConcentrationUnit());
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.Real;
	}
	
	@Override
	public final ContentType getContentType() {
		return ContentType.Concentration;
	}
	
	
	@Override
	public IConverter getDataConverterTo(final DataUnitConfig dataUnitConfig) {
		if (dataUnitConfig.getConcentrationUnit() == getConcentrationUnit()) {
			return null;
		}
		return new ConcentrationValueConverter(
				getConcentrationUnit(),
				dataUnitConfig.getConcentrationUnit() );
	}
	
}
