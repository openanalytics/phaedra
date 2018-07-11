package eu.openanalytics.phaedra.silo.jep;

import eu.openanalytics.phaedra.base.scripting.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.calculation.CalculationException;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.calculation.PlateDataAccessor;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.silo.Activator;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;

public class WellFeatureSiloScanner extends BaseScanner<SiloDataset> {

	private final static char VAR_SIGN = '#';

	@Override
	protected boolean isValidObject(Object obj) {
		return obj instanceof SiloDataset;
	}

	@Override
	protected char getVarSign() {
		return VAR_SIGN;
	}

	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, SiloDataset dataset) {
		ProtocolClass pClass = dataset.getSilo().getProtocolClass();
		Feature feature = ProtocolUtils.getFeatureByName(fieldNames[0], pClass);

		if (feature == null || !feature.isNumeric()) {
			throw new CalculationException("Feature is null or not numeric: " + fieldNames[0]);
		}

		return getValueFor(feature, dataset, scope);
	}

	private Object getValueFor(Feature feature, SiloDataset dataset, String scope) {
		Silo silo = dataset.getSilo();
		ISiloAccessor<?> accessor = SiloService.getInstance().getSiloAccessor(silo);

		try {
			int rows = accessor.getRowCount(dataset.getName());
			if (silo.getType() == GroupType.WELL.getType()) {
				float[] featureData = new float[rows];
				for (int i = 0; i < rows; i++) {
					Object row = accessor.getRowObject(dataset.getName(), i);
					if (row instanceof Well) {
						Well well = (Well) row;
						PlateDataAccessor plateAccessor = CalculationService.getInstance().getAccessor(well.getPlate());
						featureData[i] = (float) plateAccessor.getNumericValue(well, feature, scope);
					}
				}
				return toVector(featureData);
			} else if (silo.getType() == GroupType.SUBWELL.getType()) {
				float[] featureData = new float[rows];
				for (int i = 0; i < rows; i++) {
					Object row = accessor.getRowObject(dataset.getName(), i);
					if (row instanceof SubWellItem) {
						SubWellItem swItem = (SubWellItem) row;
						PlateDataAccessor plateAccessor = CalculationService.getInstance().getAccessor(swItem.getWell().getPlate());
						featureData[i] = (float) plateAccessor.getNumericValue(swItem.getWell(), feature, scope); 
					}
				}
				return toVector(featureData);
			} else {
				throw new CalculationException("Silos of this type are not supported.");
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
		return null;
	}

}