package eu.openanalytics.phaedra.model.curve.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.curve.CurveProperty;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

/**
 * Utility class to obtain user-friendly String values of curve properties.
 */
public class CurvePropertyProvider {

	public static String[] getProperties(Curve curve) {
		List<String> props = Arrays.stream(CurveProperty.values())
			.filter(p -> p.appliesTo(curve))
			.filter(p -> p != CurveProperty.PLOT && p != CurveProperty.CI_GRID && p != CurveProperty.WEIGHTS && p != CurveProperty.FIT_MESSAGE)
			.filter(p -> p != CurveProperty.PIC50_CENSOR && p != CurveProperty.PLAC_CENSOR)
			.filter(p -> p != CurveProperty.GROUP_BY_1 || (curve != null && curve.getSettings().getGroupBy1() != null))
			.filter(p -> p != CurveProperty.GROUP_BY_2 || (curve != null && curve.getSettings().getGroupBy2() != null))
			.filter(p -> p != CurveProperty.GROUP_BY_3 || (curve != null && curve.getSettings().getGroupBy3() != null))
			.map(p -> p.getLabel())
			.collect(Collectors.toList());
		return props.toArray(new String[props.size()]);
	}

	public static String getValue(String property, Curve curve) {
		CurveProperty prop = CurveProperty.getByLabel(property);
		if (prop == null) return "";
		switch (prop) {
		case PIC50:
		case PLAC:
			return CurveService.getInstance().getCurveDisplayValue(curve);
		default:
			Object value = prop.getValue(curve);
			if (value instanceof Double) return NumberUtils.round((Double)value, 2);
			if (value != null) return value.toString();
		}
		return "";
	}
}
