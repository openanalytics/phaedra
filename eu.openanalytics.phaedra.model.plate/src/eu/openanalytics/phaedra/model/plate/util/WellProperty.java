package eu.openanalytics.phaedra.model.plate.util;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.Molar;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.description.ConcentrationValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataUnitConfig;
import eu.openanalytics.phaedra.base.datatype.description.EntityIdDescription;
import eu.openanalytics.phaedra.base.datatype.description.IntegerValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.RealValueDescription;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.model.EntityProperty;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;


public enum WellProperty implements EntityProperty<Well> {
	
	Number(new IntegerValueDescription("Well Number", Well.class)),
	Position(new StringValueDescription("Well Position", Well.class)),
	Row(new IntegerValueDescription("Well Row", Well.class), "Row"),
	Column(new IntegerValueDescription("Well Column", Well.class), "Column"),
	WellType(new StringValueDescription("Well Type", Well.class)),
	Compound(new StringValueDescription("Compound", Well.class)),
	Concentration(new ConcentrationValueDescription("Concentration", Well.class, Molar)),
	LogConcentration(new RealValueDescription("Log Concentration", Well.class)),
	PlateSequence(new IntegerValueDescription("Plate Sequence", Well.class)),
	PlateId(new EntityIdDescription("Plate ID", Well.class, Plate.class)),
	ExperimentId(new EntityIdDescription("Experiment ID", Well.class, Experiment.class));
	
	
	private final String label;
	private final String shortLabel;
	
	private final DataDescription dataDescription;
	private final boolean numeric;
	
	
	private WellProperty(final DataDescription dataDescription, final String shortLabel) {
		this.label = dataDescription.getName();
		this.shortLabel= shortLabel;
		this.dataDescription = dataDescription;
		this.numeric = (dataDescription.getDataType() == DataType.Integer || dataDescription.getDataType() == DataType.Real);
	}
	
	private WellProperty(final DataDescription dataDescription) {
		this(dataDescription, dataDescription.getName());
	}
	
	
	@Override
	public String getKey() {
		return name();
	}
	
	@Override
	public String getLabel() {
		return this.label;
	}
	
	@Override
	public String getShortLabel() {
		return this.shortLabel;
	}
	
	@Override
	public DataDescription getDataDescription() {
		return this.dataDescription;
	}
	
	public boolean isNumeric() {
		return numeric;
	}

	public double getValue(Well well) {
		switch (this) {
		case Number: return PlateUtils.getWellNr(well);
		case Row: return well.getRow();
		case Column: return well.getColumn();
		case Concentration: return well.getCompoundConcentration();
		case LogConcentration: return well.getCompoundConcentration() == 0.0 ? Double.NaN : Math.log10(well.getCompoundConcentration());
		case PlateSequence: return well.getPlate().getSequence();
		case PlateId: return well.getPlate().getId();
		case ExperimentId: return well.getPlate().getExperiment().getId();
		default: return 0.0;
		}
	}

	public String getStringValue(Well well) {
		switch (this) {
		case Number:
		case Row:
		case Column:
			return "" + (int)getValue(well);
		case Position: return PlateUtils.getWellCoordinate(well);
		case WellType: return well.getWellType();
		case Compound: return well.getCompound() == null ? well.getWellType() : well.getCompound().toString();
		case Concentration: return Formatters.getInstance().format(getValue(well), "0.0#E0");
		case LogConcentration: return Formatters.getInstance().format(getValue(well), "0.00");
		default:
			return numeric ? "" + getValue(well) : "";
		}
	}
	
	public double getRealValue(final Well well, final DataUnitConfig units) {
		switch (this) {
		case Number:
			return PlateUtils.getWellNr(well);
		case Row:
			return well.getRow();
		case Column:
			return well.getColumn();
		case Concentration:
			return units.getConcentrationUnit(this.dataDescription).convert(well.getCompoundConcentration(), Molar);
		case LogConcentration:
			return well.getCompoundConcentration() == 0.0 ? Double.NaN : Math.log10(well.getCompoundConcentration());
		case PlateSequence:
			return well.getPlate().getSequence();
		case PlateId:
			return well.getPlate().getId();
		case ExperimentId:
			return well.getPlate().getExperiment().getId();
		default:
			return 0.0;
		}
	}
	
	@Override
	public Object getTypedValue(final Well well) {
		switch (this) {
		case Number:
			return Integer.valueOf(PlateUtils.getWellNr(well));
		case Row:
			return Integer.valueOf(well.getRow());
		case Column:
			return Integer.valueOf(well.getColumn());
		case Position:
			return PlateUtils.getWellCoordinate(well);
		case WellType:
			return well.getWellType();
		case Compound:
			return well.getCompound();
		case Concentration:
			return well.getCompoundConcentration();
		case LogConcentration:
			return getValue(well);
		case PlateSequence:
			return Integer.valueOf(well.getPlate().getSequence());
		case PlateId:
			return Long.valueOf(well.getPlate().getId());
		case ExperimentId:
			return Long.valueOf(well.getPlate().getExperiment().getId());
		default:
			return null;
		}
	}
	
	
	public static WellProperty getByLabel(String name) {
		for (WellProperty prop: values()) {
			if (prop.getLabel().equals(name)) return prop;
		}
		return null;
	}
	
	public static WellProperty[] getNumericProperties() {
		WellProperty[] allProperties = values();
		List<WellProperty> numericProperties = new ArrayList<>();
		for (WellProperty p: allProperties) {
			if (p.isNumeric()) numericProperties.add(p);
		}
		return numericProperties.toArray(new WellProperty[numericProperties.size()]);
	}
}
