package eu.openanalytics.phaedra.base.datatype.description;

import org.eclipse.core.databinding.conversion.IConverter;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.unit.DataUnitConfig;


public interface DataDescription {
	
	
	/**
	 * Alters this description to the specified data units, if required.
	 * 
	 * @param dataUnitConfig the units to alter to.
	 * @return the adapted data description or this one.
	 */
	DataDescription alterTo(final DataUnitConfig dataUnitConfig);
	
	
	/**
	 * Returns the name of the data.
	 * @return the name.
	 */
	String getName();
	
	
	/**
	 * Returns the data type of the data.
	 * @return the data type.
	 */
	DataType getDataType();
	
	/**
	 * Returns the content type of the data.
	 * @return the content type.
	 */
	ContentType getContentType();
	
	
	String convertNameTo(final String name, final DataUnitConfig dataUnitConfig);
	
	/**
	 * Returns a converter to convert data.
	 */
	IConverter getDataConverterTo(final DataUnitConfig dataUnitConfig);
	
	/**
	 * Converts the data to the specified unit configuration, if required.
	 */
	default Object convertDataTo(final Object data, final DataUnitConfig dataUnitConfig) {
		final IConverter converter = getDataConverterTo(dataUnitConfig);
		return (converter != null) ? converter.convert(data) : data;
	}
	
	
	/**
	 * Returns if the description is equal to this one except of names.
	 * 
	 * @param other the description to check.
	 * @return
	 */
	boolean equalsType(final DataDescription other);
	
}
