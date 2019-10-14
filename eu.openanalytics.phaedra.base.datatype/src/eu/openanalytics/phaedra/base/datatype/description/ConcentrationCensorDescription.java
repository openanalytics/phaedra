package eu.openanalytics.phaedra.base.datatype.description;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationCensorConverter;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;


public class ConcentrationCensorDescription extends AbstractConcentrationDataDescription {
	
	
	public ConcentrationCensorDescription(final String name, final ConcentrationUnit unit) {
		super(name, unit);
	}
	
	@Override
	public ConcentrationCensorDescription alterTo(final DataUnitConfig dataUnitConfig) {
		if (dataUnitConfig.getConcentrationUnit() == getConcentrationUnit()) {
			return this;
		}
		return new ConcentrationCensorDescription(getName(), dataUnitConfig.getConcentrationUnit());
	}
	
	
	@Override
	public final DataType getDataType() {
		return DataType.String;
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
		return new ConcentrationCensorConverter(
				getConcentrationUnit(),
				dataUnitConfig.getConcentrationUnit() );
	}
	
	
}
