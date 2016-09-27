package eu.openanalytics.phaedra.model.curve.util;

import java.util.function.Function;

import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.vo.Curve;

public class CurveTextProvider {

	private static final CurveTextField[] BASE_COLUMNS = {
			new CurveTextField("Id", c -> String.valueOf(c.getId())),
			new CurveTextField("Model Id", c -> c.getModelId()),
			new CurveTextField("Fit Date", c -> String.valueOf(c.getFitDate())),
			new CurveTextField("Fit Version", c -> c.getFitVersion()),
			new CurveTextField("Fit Error", c -> String.valueOf(c.getErrorCode()))
	};
	
	public static CurveTextField[] getColumns(Curve curve) {
		Value[] values = curve == null ? new Value[0] : curve.getOutputParameters();
		CurveTextField[] allFields = new CurveTextField[BASE_COLUMNS.length + values.length];
		System.arraycopy(BASE_COLUMNS, 0, allFields, 0, BASE_COLUMNS.length);
		for (int i = 0; i < values.length; i++) {
			int offset = i + BASE_COLUMNS.length;
			Value v = values[i];
			allFields[offset] = new CurveTextField(v.definition.name, c -> CurveParameter.renderValue(v, curve, null));
		}
		return allFields;
	}
	
	public static class CurveTextField {
		private String label;
		private Function<Curve, String> valueRenderer;
		
		public CurveTextField(String label, Function<Curve, String> valueRenderer) {
			this.label = label;
			this.valueRenderer = valueRenderer;
		}
		
		public String getLabel() {
			return label;
		}
		
		public String renderValue(Curve curve) {
			return valueRenderer.apply(curve);
		}
		
	}
}
