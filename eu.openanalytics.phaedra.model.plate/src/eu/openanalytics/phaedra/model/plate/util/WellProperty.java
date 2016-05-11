package eu.openanalytics.phaedra.model.plate.util;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;


public enum WellProperty {

	Number("Well Number", true),
	Position("Well Position", false),
	Row("Well Row", true),
	Column("Well Column", true),
	Type("Well Type", false),
	Compound("Compound", false),
	Concentration("Concentration", true),
	LogConcentration("Log Concentration", true),
	PlateSequence("Plate Sequence", true),
	PlateId("Plate ID", true),
	ExperimentId("Experiment ID", true)
	;

	private String label;
	private boolean numeric;

	private WellProperty(String label, boolean numeric) {
		this.label = label;
		this.numeric = numeric;
	}

	public String getLabel() {
		return label;
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
		case Type: return well.getWellType();
		case Compound: return well.getCompound() == null ? well.getWellType() : well.getCompound().toString();
		case Concentration: return Formatters.getInstance().format(getValue(well), "0.0#E0");
		case LogConcentration: return Formatters.getInstance().format(getValue(well), "0.00");
		default:
			return numeric ? "" + getValue(well) : "";
		}
	}

	public static WellProperty getByName(String name) {
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
