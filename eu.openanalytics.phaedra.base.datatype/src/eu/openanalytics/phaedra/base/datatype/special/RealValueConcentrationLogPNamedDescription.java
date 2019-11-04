package eu.openanalytics.phaedra.base.datatype.special;

import static eu.openanalytics.phaedra.base.datatype.special.ConcentrationLogPNamedValueDescription.convertName;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.description.AbstractConcentrationDataDescription;
import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


/**
 * Description for values related to concentrations with name with 'p' prefix when the
 * concentration unit is LogMolar (e.g. IC50/pIC50 LB).
 */
public class RealValueConcentrationLogPNamedDescription extends AbstractConcentrationDataDescription {
	
	
	public RealValueConcentrationLogPNamedDescription(final String name, final Class<?> entityType,
			final ConcentrationUnit unit) {
		super(name, entityType, unit);
	}
	
	@Override
	public RealValueConcentrationLogPNamedDescription alterTo(final DataUnitConfig dataUnitConfig) {
		final ConcentrationUnit toUnit = dataUnitConfig.getConcentrationUnit(this);
		if (toUnit == getConcentrationUnit()) {
			return this;
		}
		return new RealValueConcentrationLogPNamedDescription(
				convertNameTo(getName(), toUnit), getEntityType(),
				toUnit );
	}
	
	
	@Override
	public DataType getDataType() {
		return DataType.Real;
	}
	
	@Override
	public ContentType getContentType() {
		return ContentType.Other;
	}
	
	
	@Override
	public String convertNameTo(final String name, final DataUnitConfig dataUnitConfig) {
		return convertNameTo(name, dataUnitConfig.getConcentrationUnit(this));
	}
	
	protected String convertNameTo(final String name, final ConcentrationUnit unit) {
		return convertName(name, getConcentrationUnit(), unit);
	}
	
}
