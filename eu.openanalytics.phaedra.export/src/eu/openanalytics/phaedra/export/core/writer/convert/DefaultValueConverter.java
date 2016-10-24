package eu.openanalytics.phaedra.export.core.writer.convert;

public class DefaultValueConverter implements IValueConverter {

	@Override
	public String convert(String value) {
		if (value == null) return null;
		// Replace NaNs with empty values for Excel compatibility.
		if (value.equals("NaN")) return "";
		// Also replace "-" values by empty values for Excel compatibility.
		if (value.equals("-")) return "";
		// Replace line breaks by whitespace to prevent formatting issues.
		if (value.contains("\r\n")) return value.replace("\r\n", " ");
		if (value.contains("\n")) return value.replace("\n", " ");
		if (value.contains("\t")) return value.replace("\t", " ");
		
		return value;
	}

}
