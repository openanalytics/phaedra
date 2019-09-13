package eu.openanalytics.phaedra.model.curve.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.property.ObjectPropertyService;


public class CurveTextProvider {

	private static final CurveTextField[] BASE_COLUMNS = {
			new CurveTextField("Id", c -> String.valueOf(c.getId())),
			new CurveTextField("Model Id", c -> c.getModelId()),
			new CurveTextField("Fit Date", c -> String.valueOf(c.getFitDate())),
			new CurveTextField("Fit Version", c -> c.getFitVersion()),
			new CurveTextField("Fit Error", c -> String.valueOf(c.getErrorCode()))
	};
	
	public static CurveTextField[] getColumns(Curve curve) {
		// First, the base properties
		List<CurveTextField> allFields = new ArrayList<>();
		Arrays.stream(BASE_COLUMNS).forEach(c -> allFields.add(c));
		
		// Then, the curve fit properties
		Value[] values = curve == null ? new Value[0] : curve.getOutputParameters();
		Arrays.stream(values).forEach(v -> {
			allFields.add(new CurveTextField(v.definition.name, c -> CurveParameter.renderValue(v, curve, null)));
		});
		
		// Last, the custom compound properties
		if (curve != null && !curve.getCompounds().isEmpty()) {
			Compound c = curve.getCompounds().get(0);
			Map<String, Object> extraProps = ObjectPropertyService.getInstance().getProperties(Compound.class.getName(), c.getId());
			List<String> propNames = new ArrayList<>(extraProps.keySet());
			propNames.sort(null);
			for (String name: propNames) {
				allFields.add(new CurveTextField(name, cu -> String.valueOf(extraProps.get(name))));
			}
		}
		
		return allFields.toArray(new CurveTextField[allFields.size()]);
	}
	
	public static List<CurveTextField> getColumns(Curve curve, List<String> favorites) {
		CurveTextField[] columns = getColumns(curve);
		
		List<CurveTextField> unordered = new ArrayList<>();
		for (int i = 0; i < columns.length; i++) {
			unordered.add(columns[i]);
		}
		if (favorites.isEmpty()) return unordered;
		
		List<CurveTextField> ordered = new ArrayList<>(unordered.size());
		for (String name : favorites) {
			for (Iterator<CurveTextField> iter = unordered.iterator(); iter.hasNext();) {
				CurveTextField field = iter.next();
				if (field.getLabel().equals(name)) {
					ordered.add(field);
					iter.remove();
					break;
				}
			}
		}
		ordered.addAll(unordered);
		return ordered;
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
