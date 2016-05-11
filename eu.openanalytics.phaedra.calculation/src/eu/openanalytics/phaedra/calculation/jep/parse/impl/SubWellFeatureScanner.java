package eu.openanalytics.phaedra.calculation.jep.parse.impl;

import java.util.Arrays;

import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.calculation.norm.NormalizationException;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class SubWellFeatureScanner extends BaseScanner<Well> {

	private final static char VAR_SIGN = '$';
	
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
		
		SubWellFeature feature = ProtocolUtils.getSubWellFeatureByName(featureName, pClass);
		if (feature == null || !feature.isNumeric()) {
			throw new CalculationException("Feature is null or not numeric: " + featureName);
		}
		
		float[] featureData = getSubwellData(well, feature, normalization);
		return (featureData == null) ? Double.NaN : toVector(featureData);
	}
	
	private float[] getSubwellData(Well well, SubWellFeature feature, String normalization) {
		float[] featureData = SubWellService.getInstance().getNumericData(well, feature, 0, false);
		if (featureData == null || featureData.length == 0) return null;

		if (normalization != null && !normalization.isEmpty()) {
			float[] normalizedData = new float[featureData.length];
			try {
				for (int i=0; i<featureData.length; i++) {
					normalizedData[i] = (float)NormalizationService.getInstance().getNormalizedValue(well, i, feature, normalization);
				}
			} catch (NormalizationException e) {
				Arrays.fill(normalizedData, Float.NaN);
			}
			featureData = normalizedData;
		}
		
		return featureData;
	}
}
