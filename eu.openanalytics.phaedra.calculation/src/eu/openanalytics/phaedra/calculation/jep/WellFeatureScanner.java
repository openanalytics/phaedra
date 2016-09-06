package eu.openanalytics.phaedra.calculation.jep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.openanalytics.phaedra.base.scripting.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class WellFeatureScanner extends BaseScanner<Well> {

	private final static char VAR_SIGN = '#';

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
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		
		String featureName = fieldNames[0];
		String normalization = fieldNames.length > 1 ? fieldNames[1] : null;
		
		Feature feature = ProtocolUtils.getFeatureByName(featureName, pClass);
		if (feature == null || !feature.isNumeric()) {
			throw new CalculationException("Feature is null or not numeric: " + featureName);
		}

		return getValueFor(feature, well, scope, normalization);
	}

	private Object getValueFor(Feature feature, Well well, String scope, String normalization) {
		Object value = null;
		
		if (scope == null || scope.equalsIgnoreCase(SCOPE_WELL)) {
			value = getValue(well, feature, normalization);
		} else if (scope.equalsIgnoreCase(SCOPE_SUBWELL)) {
			int cellCount = 0;
			SubWellFeature swf = SubWellService.getInstance().getSampleFeature(well);
			Object data = SubWellService.getInstance().getAnyData(well, swf);
			if (data != null) {
				if (swf.isNumeric()) cellCount = ((float[])data).length;
				else cellCount = ((String[])data).length;
			}
			double[] values = new double[cellCount];
			double singleValue = getValue(well, feature, normalization);
			Arrays.fill(values, singleValue);
			value = values;
		} else if (scope.equalsIgnoreCase(SCOPE_PLATE)) {
			value = toVector(getValues(well.getPlate(), feature, normalization));
		} else if (scope.equalsIgnoreCase(SCOPE_EXPERIMENT)) {
			List<Plate> plates = getPlates(well.getPlate().getExperiment());
			List<double[]> valueList = new ArrayList<>();
			int totalSize = 0;
			for (Plate plate: plates) {
				double[] values = getValues(plate, feature, normalization);
				valueList.add(values);
				totalSize += values.length;
			}
			double[] allValues = new double[totalSize];
			int index = 0;
			for (double[] values: valueList) {
				for (double v: values) {
					allValues[index++] = v;
				}
			}
			value = toVector(allValues);
		}
		
		return value;
	}

	private double getValue(Well well, Feature feature, String normalization) {
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(well.getPlate());
		double value = accessor.getNumericValue(well, feature, normalization);
		return value;
	}
	
	private double[] getValues(Plate plate, Feature feature, String normalization) {
		PlateDataAccessor accessor = CalculationService.getInstance().getAccessor(plate);
		List<Well> wells = getWells(plate);
		double[] values = new double[wells.size()];
		for (int i=0; i<wells.size(); i++) {
			Well well = wells.get(i);
			values[i] = accessor.getNumericValue(well, feature,  normalization);
		}
		return values;
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
