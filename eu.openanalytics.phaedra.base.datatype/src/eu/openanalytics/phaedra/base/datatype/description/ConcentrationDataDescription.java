package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


/**
 * Data description of concentration values or other data related to a concentration data.
 */
public interface ConcentrationDataDescription extends DataDescription {
	
	
	@Override
	ConcentrationDataDescription alterTo(final DataUnitConfig dataUnitConfig);
	
	/**
	 * Returns the concentration unit of the data.
	 * 
	 * @return the unit.
	 */
	ConcentrationUnit getConcentrationUnit();
	
}
