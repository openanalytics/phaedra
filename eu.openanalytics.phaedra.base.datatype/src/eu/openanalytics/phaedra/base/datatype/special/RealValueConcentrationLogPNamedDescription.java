package eu.openanalytics.phaedra.base.datatype.special;

import static eu.openanalytics.phaedra.base.datatype.special.ConcentrationLogPNamedValueDescription.convertName;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.description.AbstractConcentrationDataDescription;
import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;


/**
 * Description for values related to concentrations with name with 'p' prefix when the
 * concentration unit is LogMolar (e.g. IC50/pIC50 LB).
 */
public class RealValueConcentrationLogPNamedDescription extends AbstractConcentrationDataDescription {
	
	
	public RealValueConcentrationLogPNamedDescription(final String name, final ConcentrationUnit unit) {
		super(name, unit);
	}
	
	@Override
	public RealValueConcentrationLogPNamedDescription alterTo(final DataUnitConfig dataUnitConfig) {
		final ConcentrationUnit unit = dataUnitConfig.getConcentrationUnit();
		if (unit == getConcentrationUnit()) {
			return this;
		}
		return new RealValueConcentrationLogPNamedDescription(
				convertNameTo(getName(), dataUnitConfig), unit );
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
		return convertName(name, getConcentrationUnit(), dataUnitConfig.getConcentrationUnit());
	}
	
}
