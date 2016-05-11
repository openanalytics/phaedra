package eu.openanalytics.phaedra.ui.plate.grid.layer;

import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;

public class ValueProvider {

	public static final String VALUE_TYPE_ACTIVE_FEATURE = "Active Feature";
	public static final String VALUE_TYPE_FEATURE = "Feature";
	public static final String VALUE_TYPE_PROPERTY = "Well Property";
	public static final String VALUE_TYPE_NONE = "None";
	
	public static String getValue(Well well, ValueKey key) {
		if (well == null) return "";

		switch (key.valueType) {
		case VALUE_TYPE_ACTIVE_FEATURE:
		case VALUE_TYPE_FEATURE:
			return getFeatureValue(well, (Feature) key.arg1, (String) key.arg2);
		case VALUE_TYPE_PROPERTY:
			return ((WellProperty) key.arg1).getStringValue(well);
		}
		return "";
	}

	private static String getFeatureValue(Well well, Feature f, String norm) {
		if (f.isNumeric()) {
			double value = CalculationService.getInstance().getAccessor(well.getPlate()).getNumericValue(well, f, norm);
			return Formatters.getInstance().format(value, f);
		} else {
			return CalculationService.getInstance().getAccessor(well.getPlate()).getStringValue(well, f);
		}
	}
	
public static class ValueKey {
		
		public String valueType;
		public Object arg1;
		public Object arg2;
		
		private static final String ID_SEPARATOR = "##";
		
		public ValueKey(String valueType, Object arg1, Object arg2) {
			this.valueType = valueType;
			this.arg1 = arg1;
			this.arg2 = arg2;
		}
		
		@Override
		public String toString() {
			if (VALUE_TYPE_FEATURE.equals(valueType)) {
				String label = ((IFeature) arg1).getName();
				if (arg2 != null) label += " " + arg2.toString();
				return label;
			} else if (VALUE_TYPE_PROPERTY.equals(valueType)) {
				return ((WellProperty) arg1).getLabel();
			}
			return valueType;
		}
		
		public String toIdString() {
			StringBuilder sb = new StringBuilder();
			sb.append(valueType + ID_SEPARATOR);
			if (arg1 instanceof IFeature) sb.append(((IFeature) arg1).getId());
			else if (arg1 instanceof WellProperty) sb.append(((WellProperty) arg1).getLabel());
			else sb.append(String.valueOf(arg1));
			sb.append(ID_SEPARATOR + arg2);
			return sb.toString();
		}
		
		public static ValueKey fromIdString(String string) {
			String[] parts = string.split(ID_SEPARATOR);
			if (parts.length == 0 || VALUE_TYPE_NONE.equals(parts[0])) return ValueKey.create(VALUE_TYPE_NONE);
			Object arg1 = null;
			if (parts[0].equals(VALUE_TYPE_FEATURE)) arg1 = ProtocolService.getInstance().getFeature(Long.parseLong(parts[1]));
			else if (parts[0].equals(VALUE_TYPE_PROPERTY)) arg1 = WellProperty.getByName(parts[1]);
			else if (parts.length > 1 && !"null".equals(parts[1])) arg1 = parts[1];
			String arg2 = null;
			if (parts.length > 2 && parts[2] != null && !"null".equals(parts[2])) arg2 = parts[2];
			return ValueKey.create(parts[0], arg1, arg2);
		}
		
		public static ValueKey create(String valueType, Object... args) {
			Object arg1 = (args.length > 0) ? args[0] : null;
			Object arg2 = (args.length > 1) ? args[1] : null;
			return new ValueKey(valueType, arg1, arg2);
		}
	}
}