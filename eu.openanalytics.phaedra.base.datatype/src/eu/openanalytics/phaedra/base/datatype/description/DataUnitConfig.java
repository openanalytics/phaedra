package eu.openanalytics.phaedra.base.datatype.description;

import eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit;


public interface DataUnitConfig {
	
	
	ConcentrationUnit getConcentrationUnit(final DataDescription dataDescription);
	
}
