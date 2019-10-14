package eu.openanalytics.phaedra.base.datatype.unit;


public class DataUnitConfig {
	
	
	public static final DataUnitConfig DEFAULT = new DataUnitConfig(ConcentrationUnit.Molar);
	
	
	private final ConcentrationUnit concentrationUnit;
	
	
	public DataUnitConfig(final ConcentrationUnit concentrationUnit) {
		this.concentrationUnit = concentrationUnit;
	}
	
	
	public final ConcentrationUnit getConcentrationUnit() {
		return this.concentrationUnit;
	}
	
	
}
