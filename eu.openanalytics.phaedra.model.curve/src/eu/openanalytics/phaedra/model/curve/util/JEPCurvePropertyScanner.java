package eu.openanalytics.phaedra.model.curve.util;

import eu.openanalytics.phaedra.base.scripting.jep.parse.BaseScanner;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.ParameterType;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

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
		Curve curve = CurveFitService.getInstance().getCurve(well, feature);
		if (curve == null) return Double.NaN;
		
		Value prop = CurveParameter.find(curve.getOutputParameters(), propName);
		if (prop == null) return Double.NaN;

		Object value = null;
		if (prop.definition.type == ParameterType.Binary) {
			value = CurveParameter.getBinaryValue(prop);
		} else if (prop.definition.type.isNumeric()) {
			value = prop.numericValue;
		} else {
			value = prop.stringValue;
		}

		if (value == null) value = Double.NaN;
		return value;
	}
}
