package eu.openanalytics.phaedra.export.core.writer;

import eu.openanalytics.phaedra.export.core.writer.convert.DefaultValueConverter;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;
import eu.openanalytics.phaedra.export.core.writer.format.CSVWriter;
import eu.openanalytics.phaedra.export.core.writer.format.StreamingXLSXWriter;
import eu.openanalytics.phaedra.export.core.writer.format.TXTWriter;

public class WriterFactory {

	private static IValueConverter valueConverter = new DefaultValueConverter();
	
	public static final String XLSX_FILE_TYPE = "xlsx";
	public static final String CSV_FILE_TYPE = "csv";
	public static final String TXT_FILE_TYPE = "txt";
	public static final String H5_FILE_TYPE = "h5";
	
	public static String[] getAvailableFileTypes() {
		return new String[] {
				XLSX_FILE_TYPE,
				CSV_FILE_TYPE,
				TXT_FILE_TYPE };
	}
	
	public static String getFileTypeName(String fileType) {
		switch (fileType) {
		case XLSX_FILE_TYPE:
			return "Excel Workbook";
		case CSV_FILE_TYPE:
			return "Comma-separated Values";
		case TXT_FILE_TYPE:
			return "Tab-separated Text";
		default:
			return fileType.toUpperCase();
		}
	}
	
	public static IExportWriter createWriter(String fileType) {
		IExportWriter writer = null;
		
		switch (fileType) {
		case XLSX_FILE_TYPE:
			writer = new StreamingXLSXWriter();
			break;
		case CSV_FILE_TYPE:
			writer = new CSVWriter();
			break;
		case TXT_FILE_TYPE:
			writer = new TXTWriter();
			break;
		default:
			return null;
		}
		
		if (writer != null) {
			writer.setValueConverter(valueConverter);
		}
		
		return writer;
	}
	
	public static String applyFileType(String path, String fileType) {
		while (fileType.startsWith("*") || fileType.startsWith(".")) fileType = fileType.substring(1);
		if (path.endsWith(fileType)) return path;
		return removeFileType(path) + "." + fileType;
	}
	
	public static String removeFileType(String path) {
		int index = path.lastIndexOf(".");
		if (index == -1) return path;
		return path.substring(0, index);
	}
	
}
