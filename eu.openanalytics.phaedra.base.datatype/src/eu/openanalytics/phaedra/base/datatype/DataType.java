package eu.openanalytics.phaedra.base.datatype;


public enum DataType {
	
	/**
	 * Type for string values.
	 **/
	String("String"),
	
	/**
	 * Type for boolean values.
	 **/
	Boolean("Boolean"),
	
	/**
	 * Type for integer values.
	 **/
	Integer("Integer"),
	
	/**
	 * Type for floating point values.
	 **/
	Real("Number"),
	
	/**
	 * Type for byte array values (BLOB).
	 **/
	ByteArray("Byte Array"),
	
	
	/**
	 * Type for date/time values.
	 **/
	DateTime("Date/Time"),
	
	/**
	 * Type for images.
	 */
	Image("Image");
	
	
	private final String label;
	
	
	private DataType(final String label) {
		this.label = label;
	}
	
	
	public String getLabel() {
		return this.label;
	}
	
	
}
