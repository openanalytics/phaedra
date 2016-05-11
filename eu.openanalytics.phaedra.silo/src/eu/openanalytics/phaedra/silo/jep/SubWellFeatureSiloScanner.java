package eu.openanalytics.phaedra.silo.jep;

import java.util.Arrays;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.silo.Activator;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.silo.vo.Silo;

public class SubWellFeatureSiloScanner extends BaseScanner<SiloStructure> {

	private final static char VAR_SIGN = '$';

	@Override
	protected boolean isValidObject(Object obj) {
		return obj instanceof SiloStructure;
	}

	@Override
	protected char getVarSign() {
		return VAR_SIGN;
	}

	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, SiloStructure group) {
		ProtocolClass pClass = group.getSilo().getProtocolClass();
		SubWellFeature feature = ProtocolUtils.getSubWellFeatureByName(fieldNames[0], pClass);

		if (feature == null) {
			throw new CalculationException("Feature was not found: " + fieldNames[0]);
		} else if (!feature.isNumeric()) {
			throw new CalculationException("Feature was not numeric: " + fieldNames[0]);
		}

		float[] featureData = getSubwellData(group, feature, scope);
		return (featureData == null) ? Double.NaN : toVector(featureData);
	}

	private float[] getSubwellData(SiloStructure group, SubWellFeature feature, String scope) {
		boolean hasScope = scope != null && !scope.isEmpty();

		Silo silo = group.getSilo();
		String dataGroup = group.getFullName();
		ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(silo);
		try {
			int rows = accessor.getRowCount(dataGroup);
			if (silo.getType() == GroupType.WELL.getType()) {
				float[] subWellFeatureData = new float[0];
				for (int i = 0; i < rows; i++) {
					Object row = accessor.getRow(dataGroup, i);
					if (row instanceof Well) {
						Well well = (Well) row;
						float[] featureData = SubWellService.getInstance().getNumericData(well, feature);
						int currentData = subWellFeatureData.length;
						int newData = featureData.length;
						if (featureData != null && newData > 0) {
							Arrays.copyOf(subWellFeatureData, currentData + newData);
							for (int j = 0; j < newData; j++) {
								if (hasScope) {
									subWellFeatureData[i] = (float) NormalizationService.getInstance().getNormalizedValue(well, j, feature, scope);
								} else {
									subWellFeatureData[currentData + j] = featureData[j];
								}
							}
						}
					}
				}
				return subWellFeatureData;
			} else if (silo.getType() == GroupType.SUBWELL.getType()) {
				float[] subWellFeatureData = new float[rows];
				Arrays.fill(subWellFeatureData, Float.NaN);
				for (int i = 0; i < rows; i++) {
					Object row = accessor.getRow(dataGroup, i);
					if (row instanceof SubWellItem) {
						SubWellItem swItem = (SubWellItem) row;
						float[] featureData = SubWellService.getInstance().getNumericData(swItem.getWell(), feature);
						if (featureData != null && featureData.length > swItem.getIndex()) {
							subWellFeatureData[i] = featureData[swItem.getIndex()];
						}

						if (hasScope) {
							subWellFeatureData[i] = (float) NormalizationService.getInstance().getNormalizedValue(swItem.getWell(), swItem.getIndex(), feature, scope);
						}
					}
				}
				return subWellFeatureData;
			} else {
				throw new CalculationException("Silos of this type are not supported.");
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
		return null;
	}

}