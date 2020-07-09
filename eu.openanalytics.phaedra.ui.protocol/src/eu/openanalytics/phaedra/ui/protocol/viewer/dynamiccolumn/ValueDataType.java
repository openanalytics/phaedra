package eu.openanalytics.phaedra.ui.protocol.viewer.dynamiccolumn;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import eu.openanalytics.phaedra.base.datatype.description.ConcentrationDataDescription;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;


public class ValueDataType {
	
	
	private final String label;
	
	private final BiFunction<String, Class<?>, DataDescription> dataDescriptor;
	
	private final String key;
	
	private final List<ValueFormat> supportedFormats;
	
	
	public ValueDataType(final String label, final BiFunction<String, Class<?>, DataDescription> dataDescriptor,
			final List<ValueFormat> supportedFormats) {
		this.label = label;
		this.dataDescriptor = dataDescriptor;
		this.key= createKey(createDataDescription("", Object.class));
		
		this.supportedFormats = supportedFormats;
	}
	
	public ValueDataType(final String label, final BiFunction<String, Class<?>, DataDescription> dataDescriptor,
			final ValueFormat... supportedFormats) {
		this(label, dataDescriptor, Arrays.asList(supportedFormats));
	}
	
	protected String createKey(final DataDescription dataDescription) {
		if (dataDescription == null) {
			return "auto";
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(dataDescription.getDataType().name());
		sb.append(':');
		sb.append(dataDescription.getContentType().name());
		if (dataDescription instanceof ConcentrationDataDescription) {
			sb.append(':');
			sb.append(((ConcentrationDataDescription)dataDescription).getConcentrationUnit().name());
		}
		return sb.toString();
	}
	
	
	public String getLabel() {
		return this.label;
	}
	
	public DataDescription createDataDescription(final String name, final Class<?> entityType) {
		return this.dataDescriptor.apply(name, entityType);
	}
	
	public List<? extends ValueFormat> getSupportedFormats() {
		return this.supportedFormats;
	}
	
	
	@Override
	public int hashCode() {
		return this.key.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof ValueDataType) {
			return this.key.equals(((ValueDataType)obj).key);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return this.key;
	}
	
	
	public static ValueDataType getType(final List<? extends ValueDataType> list, final String key) {
		if (key != null) {
			for (final ValueDataType type : list) {
				if (type.key.equals(key)) {
					return type;
				}
			}
		}
		return list.get(0);
	}
	
}
