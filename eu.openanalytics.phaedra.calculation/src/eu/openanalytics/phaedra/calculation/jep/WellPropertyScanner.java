package eu.openanalytics.phaedra.calculation.jep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.base.scripting.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class WellPropertyScanner extends BaseScanner<Well> {

	private final static char VAR_SIGN = '@';

	@Override
	protected boolean isValidObject(Object obj) {
		return obj instanceof Well;
	}

	@Override
	protected char getVarSign() {
		return VAR_SIGN;
	}

	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, Well well) {
		WellProperty prop = WellProperty.getByName(fieldNames[0]);
		if (prop == null) throw new CalculationException("No well property with name: " + fieldNames[0]);

		Object value = null;
		if (scope == null || scope.equalsIgnoreCase(SCOPE_WELL)) {
			if (prop.isNumeric()) {
				value = prop.getValue(well);
			} else {
				value = prop.getStringValue(well);
			}
		}
		else if (scope.equalsIgnoreCase(SCOPE_SUBWELL)) {
			int cellCount = 0;
			SubWellFeature swf = SubWellService.getInstance().getSampleFeature(well);
			Object data = SubWellService.getInstance().getAnyData(well, swf);
			if (data != null) {
				if (swf.isNumeric()) cellCount = ((float[])data).length;
				else cellCount = ((String[])data).length;
			}

			if (prop.isNumeric()) {
				double[] values = new double[cellCount];
				double numValue = prop.getValue(well);
				Arrays.fill(values, numValue);
				value = toVector(values);
			} else {
				String[] values = new String[cellCount];
				String stringValue = prop.getStringValue(well);
				Arrays.fill(values, stringValue);
				value = toVector(values);
			}
		}
		else if (scope.equalsIgnoreCase(SCOPE_PLATE)) {
			List<Well> wells = getWells(well.getPlate());
			if (prop.isNumeric()) {
				double[] values = new double[wells.size()];
				for (int i=0; i<values.length; i++) {
					values[i] = prop.getValue(wells.get(i));
				}
				value = toVector(values);
			} else {
				String[] values = new String[wells.size()];
				for (int i=0; i<values.length; i++) {
					values[i] = prop.getStringValue(wells.get(i));
				}
				value = toVector(values);
			}
		}
		else if (scope.equalsIgnoreCase(SCOPE_EXPERIMENT)) {
			List<Plate> plates = getPlates(well.getPlate().getExperiment());
			if (prop.isNumeric()) {
				List<Double> values = new ArrayList<>();
				for (Plate plate: plates) {
					List<Well> wells = getWells(plate);
					for (Well w: wells) {
						values.add(prop.getValue(w));
					}
				}
				value = toVector(CollectionUtils.toArray(values));
			} else {
				List<String> values = new ArrayList<>();
				for (Plate plate: plates) {
					List<Well> wells = getWells(plate);
					for (Well w: wells) {
						values.add(prop.getStringValue(w));
					}
				}
				value = toVector(values.toArray(new String[values.size()]));
			}
		}
		return value;
	}

	private List<Well> getWells(Plate plate) {
		// Get wells, sorted by WELLNR.
		List<Well> wells = new ArrayList<>(plate.getWells());
		Collections.sort(wells, PlateUtils.WELL_NR_SORTER);
		return wells;
	}

	private List<Plate> getPlates(Experiment exp) {
		// Get plates, sorted by ID.
		List<Plate> plates = new ArrayList<>(PlateService.getInstance().getPlates(exp));
		Collections.sort(plates, PlateUtils.PLATE_ID_SORTER);
		return plates;
	}
}
