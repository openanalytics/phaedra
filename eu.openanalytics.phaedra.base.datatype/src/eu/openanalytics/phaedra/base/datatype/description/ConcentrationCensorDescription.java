package eu.openanalytics.phaedra.base.datatype.description;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationCensorConverter;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


public class ConcentrationCensorDescription extends AbstractConcentrationDataDescription {
	
	
	public ConcentrationCensorDescription(final String name, final Class<?> entityType,
			final ConcentrationUnit unit) {
		super(name, entityType, unit);
	}
	
	@Override
	public ConcentrationCensorDescription alterTo(final DataUnitConfig dataUnitConfig) {
		final ConcentrationUnit toUnit = dataUnitConfig.getConcentrationUnit(this);
		if (toUnit == getConcentrationUnit()) {
			return this;
		}
		return new ConcentrationCensorDescription(getName(), getEntityType(),
				toUnit );
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
		final ConcentrationUnit toUnit = dataUnitConfig.getConcentrationUnit(this);
		if (toUnit == getConcentrationUnit()) {
			return null;
		}
		return new ConcentrationCensorConverter(getConcentrationUnit(), toUnit);
	}
	
	
}
