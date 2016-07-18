package eu.openanalytics.phaedra.export.core.writer;

import eu.openanalytics.phaedra.base.util.process.ProcessUtils;
import eu.openanalytics.phaedra.export.core.writer.convert.DefaultValueConverter;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;
import eu.openanalytics.phaedra.export.core.writer.format.CSVWriter;
import eu.openanalytics.phaedra.export.core.writer.format.StreamingXLSXWriter;
import eu.openanalytics.phaedra.export.core.writer.format.TXTWriter;

public class WriterFactory {

	private static IValueConverter valueConverter = new DefaultValueConverter();
	
	public static String[] getAvailableFileTypes() {
		if (ProcessUtils.isWindows()) {
			return new String[]{ "xlsx","csv","txt" };
		} else {
			return new String[]{ "*.xlsx","*.csv","*.txt" };
		}
	}
	
	public static String[] getAvailableFileTypeNames() {
		return new String[]{
				"Excel Workbook (*.xlsx)",
				"Comma-separated Values (*.csv)",
				"Tab-separated Text (*.txt)"};
	}
	
	public static IExportWriter createWriter(String type) {
		IExportWriter writer = null;
		
		if (type.endsWith("xlsx")) {
			writer = new StreamingXLSXWriter();
		} else if (type.endsWith("csv")) {
			writer = new CSVWriter();
		} else if (type.endsWith("txt")) {
			writer = new TXTWriter();
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
