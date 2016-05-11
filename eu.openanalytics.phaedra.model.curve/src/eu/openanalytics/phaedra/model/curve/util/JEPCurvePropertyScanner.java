package eu.openanalytics.phaedra.model.curve.util;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.calculation.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.model.curve.CurveProperty;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.validation.ValidationService.WellStatus;

/**
 * Enable the use of curve properties in JEP expressions of the form:
 * <pre>
 * %featureName->propertyName%
 * %AVG Size->Hill%
 * </pre>
 */
public class JEPCurvePropertyScanner extends BaseScanner<Well> {

	private final static char VAR_SIGN = '%';
	
	@Override
	protected char getVarSign() {
		return VAR_SIGN;
	}

	@Override
	protected boolean isValidObject(Object obj) {
		return obj instanceof Well;
	}
	
	@Override
	protected Object getValueForRef(String scope, String[] fieldNames, Well well) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		
		String featureName = fieldNames[0];
		String propName = fieldNames[1];
		
		Feature feature = ProtocolUtils.getFeatureByName(featureName, pClass);
		Curve curve = CurveService.getInstance().getCurve(well, feature);
		CurveProperty prop = CurveProperty.getByLabel(propName);
		if (curve == null || prop == null) return Double.NaN;

		Object value = prop.getValue(curve);
		if (value instanceof double[]) {
			double[] v = (double[]) value;
			List<Well> wells = well.getCompound().getWells();
			List<Well> filteredWells = new ArrayList<>();
			for (int i=0; i<wells.size(); i++) {
				if (wells.get(i).getStatus() != WellStatus.REJECTED_OUTLIER_PHAEDRA.getCode()) filteredWells.add(wells.get(i));
			}
			int index = filteredWells.indexOf(well);
			if (index >= 0 && index < v.length) value = v[index];
			else value = null;
		}
		
		if (value == null) value = Double.NaN;
		return value;
	}
}
