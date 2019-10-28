package eu.openanalytics.phaedra.base.datatype.description;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationValueConverter;


public class ConcentrationValueDescription extends AbstractConcentrationDataDescription {
	
	
	public ConcentrationValueDescription(final String name, final Class<?> entityType,
			final ConcentrationUnit unit) {
		super(name, entityType, unit);
	}
	
	@Override
	public ConcentrationValueDescription alterTo(final DataUnitConfig dataUnitConfig) {
		final ConcentrationUnit toUnit = dataUnitConfig.getConcentrationUnit(this);
		if (toUnit == getConcentrationUnit()) {
			return this;
		}
		return new ConcentrationValueDescription(getName(), getEntityType(),
				toUnit );
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
		final ConcentrationUnit toUnit = dataUnitConfig.getConcentrationUnit(this);
		if (toUnit == getConcentrationUnit()) {
			return null;
		}
		return new ConcentrationValueConverter(getConcentrationUnit(), toUnit);
	}
	
}
