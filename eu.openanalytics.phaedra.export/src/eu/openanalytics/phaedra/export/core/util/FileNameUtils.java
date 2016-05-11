package eu.openanalytics.phaedra.export.core.util;


public class FileNameUtils {

	public final static char ESCAPE_CHAR = '_';
	
	public static String generateName(String fileName, String featureName) {
		String escapedFName = escape(featureName);
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) {
			return fileName + "_" + escapedFName;
		} else {
			return fileName.substring(0,dotIndex) + "_" + escapedFName + fileName.substring(dotIndex);
		}
	}
	
	public static String escape(String fileName) {
		String escaped = fileName.replace('/',ESCAPE_CHAR);
		escaped = escaped.replace('?',ESCAPE_CHAR);
		escaped = escaped.replace('<',ESCAPE_CHAR);
		escaped = escaped.replace('>',ESCAPE_CHAR);
		escaped = escaped.replace('\\',ESCAPE_CHAR);
		escaped = escaped.replace(':',ESCAPE_CHAR);
		escaped = escaped.replace('*',ESCAPE_CHAR);
		escaped = escaped.replace('|',ESCAPE_CHAR);
		return escaped;
	}
}
