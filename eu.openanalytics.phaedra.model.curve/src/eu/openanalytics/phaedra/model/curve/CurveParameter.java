package eu.openanalytics.phaedra.model.curve;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.description.ConcentrationDataDescription;
import eu.openanalytics.phaedra.base.datatype.description.ContentType;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.protocol.util.Formatters;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class CurveParameter {

	public static class Definition {
		
		private final DataDescription dataDescription;
		public String name;
		public String description;
		public boolean key;
		public ParameterType type;
		private final IParameterValueRenderer valueRenderer;
		/** Supported: ParameterValueList, org.eclipse.core.databinding.validation.IValidator<String>, org.eclipse.core.databinding.conversion.IConverter<String, String> */
		public Object valueRestriction;
		
		public Definition(final DataDescription dataDescription, String description, boolean key,
				IParameterValueRenderer valueRenderer, Object valueRestriction) {
			this.dataDescription = dataDescription;
			this.name = dataDescription.getName();
			this.description = description;
			this.key = key;
			this.type = ParameterType.get(dataDescription);
			this.valueRenderer = valueRenderer;
			this.valueRestriction = valueRestriction;
		}
		
		public Definition(final DataDescription dataDescription) {
			this(dataDescription, null, false, null, null);
		}
		
		
		public final DataDescription getDataDescription() {
			return this.dataDescription;
		}
		
		public final String getName() {
			return this.name;
		}
		
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Definition other = (Definition) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (type != other.type)
				return false;
			return true;
		}
	}
	
	public static class Value {
		
		public Definition definition;
		public String stringValue;
		public double numericValue;
		public byte[] binaryValue;
		
		public Value(Definition definition, String stringValue, double numericValue, byte[] binaryValue) {
			this.definition = definition;
			this.stringValue = stringValue;
			this.numericValue = numericValue;
			this.binaryValue = binaryValue;
		}
		
		public Value(Value org) {
			this.definition = org.definition;
			this.stringValue = org.stringValue;
			this.numericValue = org.numericValue;
			this.binaryValue = org.binaryValue;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(binaryValue);
			result = prime * result + ((definition == null) ? 0 : definition.hashCode());
			long temp;
			temp = Double.doubleToLongBits(numericValue);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Value other = (Value) obj;
			if (!Arrays.equals(binaryValue, other.binaryValue))
				return false;
			if (definition == null) {
				if (other.definition != null)
					return false;
			} else if (!definition.equals(other.definition))
				return false;
			if (Double.doubleToLongBits(numericValue) != Double.doubleToLongBits(other.numericValue))
				return false;
			if (stringValue == null) {
				if (other.stringValue != null)
					return false;
			} else if (!stringValue.equals(other.stringValue))
				return false;
			return true;
		}
	}
	
	/** deprecated use {@link DataDescription} instead */
	public enum ParameterType {
		String,
		Numeric,
		Concentration,
		Binary;
		
		public static ParameterType get(DataDescription dataDescription) {
			switch (dataDescription.getDataType()) {
			case Integer:
			case Real:
				if (dataDescription.getContentType() == ContentType.Concentration) {
					return Concentration;
				}
				return Numeric;
			case ByteArray:
				return Binary;
			default:
				return String;
			}
		}
		
		public boolean isNumeric() {
			return (this == Numeric || this == Concentration);
		}
	}
	
	/*-- Value Renderer --*/
	
	public static interface IParameterValueRenderer {
		public String render(Value value, Curve curve, DataFormatter format);
	}

	public static class BaseValueRenderer implements IParameterValueRenderer {
		@Override
		public String render(Value value, Curve curve, DataFormatter formatter) {
			DataDescription dataDescription = value.definition.getDataDescription();
			switch (dataDescription.getDataType()) {
			case Integer:
				return formatter.format((long)value.numericValue, dataDescription);
			case Real:
				if (dataDescription.getContentType() == ContentType.Concentration) {
					if (formatter == null) formatter = DataTypePrefs.getDefaultDataFormatter();
					return formatter.getConcentrationFormat(dataDescription).format(getCensor(curve), value.numericValue,
							((ConcentrationDataDescription)dataDescription).getConcentrationUnit() );
				}
				return Formatters.getInstance().format(
						(double)dataDescription.convertDataTo(value.numericValue, formatter),
						"#.##" );
			case String:
				return formatter.format(value.stringValue, dataDescription);
			default:
				return value.stringValue;
			}
		}
		
		protected String getCensor(Curve curve) {
			return null;
			
		}
	}

	public static class CensoredValueRenderer extends BaseValueRenderer {
		
		private String censorParameterName;
		
		public CensoredValueRenderer(String censorParameterName) {
			this.censorParameterName = censorParameterName;
		}
		
		@Override
		protected String getCensor(Curve curve) {
			Value censor = find(curve.getOutputParameters(), censorParameterName);
			return (censor == null) ? null : censor.stringValue;
		}
	}
	
	private static final IParameterValueRenderer DEFAULT_RENDERER = new BaseValueRenderer();
	
	
	/*-- Value Restriction --*/
	
	public static class ParameterValueList {
		
		private String[] allowedValues;
		
		public ParameterValueList(String... allowedValues) {
			this.allowedValues = allowedValues;
		}
		
		public String[] getAllowedValues() {
			return allowedValues;
		}
		
	}
	
	public static class NumericValueValidator implements IValidator {
		
		private String name;
		
		public NumericValueValidator(String name) {
			this.name = name;
		}
		
		@Override
		public IStatus validate(Object value) {
			String input = (String)value;
			if (!input.isEmpty() && !NumberUtils.isNumeric(input)) {
				return ValidationStatus.error(String.format("Enter a number for %1$s.", name));
			}
			return ValidationStatus.ok();
		}
		
	}
	
	
	public static int indexOf(List<Definition> defs, String name) {
		for (int i = 0; i < defs.size(); i++) {
			if (defs.get(i).name.equals(name)) {
				return i;
			}
		}
		return -1;
	}
	
	public static Definition find(List<Definition> defs, String name) {
		int idx = indexOf(defs, name);
		return (idx >= 0) ? defs.get(idx) : null;
	}
	
	public static Value createValue(Feature feature, Definition definition) {
		String strValue = feature.getCurveSettings().get(definition.name);
		return createValue(strValue, definition);
	}
	
	public static Value createValue(String stringValue, Definition definition) {
		switch (definition.type) {
		case String:
			return new Value(definition, stringValue, Double.NaN, null);
		case Numeric:
		case Concentration:
			double numValue = (stringValue == null || stringValue.isEmpty()) ? Double.NaN : Double.parseDouble(stringValue);
			return new Value(definition, null, numValue, null);
		default:
			// Note: input parameters cannot be binary. Only output parameters, see CurveDAO.
			return new Value(definition, null, Double.NaN, null);
		}
	}
	
	public static String getValueAsString(Value value) {
		switch (value.definition.type) {
		case String:
			return value.stringValue;
		case Numeric:
		case Concentration:
			return (Double.isNaN(value.numericValue)) ? null : String.valueOf(value.numericValue);
		default:
			// Note: input parameters cannot be binary. Only output parameters, see CurveDAO.
			return null;
		}
	}
	
	public static void setValueFromString(Value value, String strValue) {
		switch (value.definition.type) {
		case String:
			value.stringValue = strValue;
			break;
		case Numeric:
		case Concentration:
			value.numericValue = Double.NaN;
			if (!strValue.isEmpty() && NumberUtils.isNumeric(strValue)) value.numericValue = Double.parseDouble(strValue);
			break;
		default:
		}
	}
	
	public static void setBinaryValue(Value value, Object binaryValue) {
		value.binaryValue = serialize(binaryValue);
	}
	
	public static Object getBinaryValue(Value value) {
		return deserialize(value.binaryValue);
	}
	
	public static Value find(Value[] values, String name) {
		return Arrays.stream(values).filter(v -> v.definition.name.equals(name)).findAny().orElse(null);
	}
	
	public static String renderValue(Value value, Curve curve, DataFormatter format) {
		IParameterValueRenderer renderer = value.definition.valueRenderer;
		if (renderer == null) renderer = DEFAULT_RENDERER;
		return renderer.render(value, curve, format);
	}
	
	private static byte[] serialize(Object data) {
		if (data == null) return null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(data);
			return bos.toByteArray();
		} catch (IOException e) {
			return null;
		}
	}

	private static Object deserialize(byte[] data) {
		if (data == null) return null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			ObjectInputStream iis = new ObjectInputStream(bis);
			return iis.readObject();
		} catch (Exception e) {
			return null;
		}
	}
}
