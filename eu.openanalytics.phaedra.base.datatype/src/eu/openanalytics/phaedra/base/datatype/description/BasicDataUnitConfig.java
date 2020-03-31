package eu.openanalytics.phaedra.base.datatype.description;

import java.util.Collections;
import java.util.Map;

import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


public class BasicDataUnitConfig implements DataUnitConfig {
	
	
	private final ConcentrationUnit defaultConcentrationUnit;
	
	private final Map<String, ConcentrationUnit> typeConcentrationUnits;
	
	
	public BasicDataUnitConfig(final ConcentrationUnit concentrationUnit,
			final Map<String, ConcentrationUnit> typeConcentrationUnits) {
		this.defaultConcentrationUnit = concentrationUnit;
		this.typeConcentrationUnits = (typeConcentrationUnits != null) ? typeConcentrationUnits : Collections.emptyMap();
	}
	
	
	@Override
	public ConcentrationUnit getConcentrationUnit(final DataDescription dataDescription) {
		ConcentrationUnit unit = null;
		if (dataDescription != null) {
			unit = this.typeConcentrationUnits.get(dataDescription.getEntityType().getName());
		}
		return (unit != null) ? unit : this.defaultConcentrationUnit;
	}
	
}
