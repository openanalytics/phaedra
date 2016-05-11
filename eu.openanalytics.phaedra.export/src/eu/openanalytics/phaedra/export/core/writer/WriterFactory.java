package eu.openanalytics.phaedra.export.core.writer;

import eu.openanalytics.phaedra.export.core.writer.convert.DefaultValueConverter;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;
import eu.openanalytics.phaedra.export.core.writer.format.CSVWriter;
import eu.openanalytics.phaedra.export.core.writer.format.StreamingXLSXWriter;
import eu.openanalytics.phaedra.export.core.writer.format.TXTWriter;

public class WriterFactory {

	private static IValueConverter valueConverter = new DefaultValueConverter();
	
	public static String[] getAvailableFileTypes() {
		return new String[]{ "xlsx","csv","txt" };
	}
	
	public static String[] getAvailableFileTypeNames() {
		return new String[]{
				"Excel Workbook (*.xlsx)",
				"Comma-separated Values (*.csv)",
				"Tab-separated Text (*.txt)"};
	}
	
	public static IExportWriter createWriter(String type) {
		IExportWriter writer = null;
		
		if (type.equals("xlsx")) {
			writer = new StreamingXLSXWriter();
		} else if (type.equals("csv")) {
			writer = new CSVWriter();
		} else if (type.equals("txt")) {
			writer = new TXTWriter();
		}
		
		if (writer != null) {
			writer.setValueConverter(valueConverter);
		}
		
		return writer;
	}
}
