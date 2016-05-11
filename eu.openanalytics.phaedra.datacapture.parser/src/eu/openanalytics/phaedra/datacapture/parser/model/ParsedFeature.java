package eu.openanalytics.phaedra.datacapture.parser.model;


public class ParsedFeature {

	private String stringValue;
	private Float numericValue;
	
	public String getStringValue() {
		return stringValue;
	}
	
	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}
	
	public Float getNumericValue() {
		return numericValue;
	}
	
	public void setNumericValue(Float numericValue) {
		this.numericValue = numericValue;
	}
}
