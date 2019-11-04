package eu.openanalytics.phaedra.model.curve.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.model.curve.CurveParameter;
import eu.openanalytics.phaedra.model.curve.CurveParameter.Value;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.property.ObjectPropertyService;


public class CurveTextProvider {

	private static final List<CurveTextField> BASE_COLUMNS = Arrays.asList(
			new CurveTextField("Id", c -> String.valueOf(c.getId())),
			new CurveTextField("Model Id", c -> c.getModelId()),
			new CurveTextField("Fit Date", c -> String.valueOf(c.getFitDate())),
			new CurveTextField("Fit Version", c -> c.getFitVersion()),
			new CurveTextField("Fit Error", c -> String.valueOf(c.getErrorCode())) );
	

	public static List<CurveTextField> getColumns(Curve curve, DataFormatter dataFormatter) {
		// First, the base properties
		List<CurveTextField> allFields = new ArrayList<>();
		allFields.addAll(BASE_COLUMNS);
		
		// Then, the curve fit properties
		Value[] curveValues = curve == null ? new Value[0] : curve.getOutputParameters();
		for (Value value : curveValues) {
			allFields.add(new CurveTextField(value.definition.getDataDescription(),
					value.definition.getDataDescription().convertNameTo(value.definition.name, dataFormatter),
					c -> CurveParameter.renderValue(value, curve, dataFormatter) ));
		}
		
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
		
		return allFields;
	}
	
	public static List<CurveTextField> getColumns(Curve curve, List<String> favorites, DataFormatter dataFormatter) {
		List<CurveTextField> columns = getColumns(curve, dataFormatter);
		
		if (favorites.isEmpty()) return columns;
		
		List<CurveTextField> ordered = new ArrayList<>(columns.size());
		for (String name : favorites) {
			for (Iterator<CurveTextField> iter = columns.iterator(); iter.hasNext();) {
				CurveTextField field = iter.next();
				if (field.getDataDescription().getName().equals(name)) {
					ordered.add(field);
					iter.remove();
					break;
				}
			}
		}
		ordered.addAll(columns);
		return ordered;
	}
	
	
	public static class CurveTextField {
		
		private final DataDescription dataDescription;
		private String label;
		private Function<Curve, String> valueRenderer;
		
		public CurveTextField(DataDescription dataDescription, String label, Function<Curve, String> valueRenderer) {
			this.dataDescription = dataDescription;
			this.label = label;
			this.valueRenderer = valueRenderer;
		}
		
		public CurveTextField(String label, Function<Curve, String> valueRenderer) {
			this(new StringValueDescription(label, Curve.class), label, valueRenderer);
		}
		
		
		public DataDescription getDataDescription() {
			return this.dataDescription;
		}
		
		public String getLabel() {
			return label;
		}
		
		public String renderValue(Curve curve) {
			return valueRenderer.apply(curve);
		}
		
	}
}
