package eu.openanalytics.phaedra.export.core.writer.format;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.ExportSettings.Includes;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.statistics.Statistics;
import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;

public class CSVWriter implements IExportWriter {
	
	private char columnSeparator;
	private char quoteChar;
	private char escapeChar;
	
	private ExportSettings settings;
	private IValueConverter valueConverter;
	
	private QueryResult baseResult;
	private List<QueryResult> featureResults;
	
	public CSVWriter() {
		this(',', au.com.bytecode.opencsv.CSVWriter.NO_QUOTE_CHARACTER, au.com.bytecode.opencsv.CSVWriter.NO_ESCAPE_CHARACTER);
	}
	
	public CSVWriter(char columnSeparator, char quoteChar, char escapeChar) {
		this.columnSeparator = columnSeparator;
		this.quoteChar = quoteChar;
		this.escapeChar = escapeChar;
	}
	
	@Override
	public void initialize(ExportSettings settings) throws IOException {
		this.settings = settings;
		this.featureResults = new ArrayList<>();
	}
	
	@Override
	public void setValueConverter(IValueConverter valueConverter) {
		this.valueConverter = valueConverter;
	}
	
	@Override
	public void writeBaseData(QueryResult result) throws IOException {
		baseResult = result;
	}
	
	@Override
	public void writeFeature(QueryResult result) throws IOException {
		featureResults.add(result);
	}
	
	@Override
	public void finish() throws IOException {
		writeFiles();
	}
	
	@Override
	public void rollback() {
		// Nothing to do
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	protected boolean needQuoting() {
		return true;
	}

	private void writeFiles() throws IOException {
		String destination = settings.destinationPath;
		try (au.com.bytecode.opencsv.CSVWriter writer = new au.com.bytecode.opencsv.CSVWriter(new FileWriter(destination), columnSeparator, quoteChar, escapeChar)) {

			int colCount = baseResult.getColumns().length + featureResults.stream().mapToInt(qr -> qr.getColumns().length).sum();
			int colIndex = 0;
			String[] rowData = new String[colCount];
			for (int c = 0; c < baseResult.getColumns().length; c++) rowData[colIndex++] = baseResult.getColumns()[c];
			for (QueryResult res: featureResults) {
				for (int c = 0; c < res.getColumns().length; c++) rowData[colIndex++] = res.getColumns()[c];
			}
			writer.writeNext(rowData);
			
			for (int r = 0; r < baseResult.getRowCount(); r++) {
				colIndex = 0;
				for (int c = 0; c < baseResult.getColumns().length; c++) {
					boolean numeric = baseResult.isColumnNumeric(c);
					if (numeric) {
						if (baseResult.getColumns()[c].endsWith("_ID")) rowData[colIndex++] = "" + Double.valueOf(baseResult.getNumericValue(r, c)).longValue();
						else rowData[colIndex++] = formatValue(baseResult.getNumericValue(r, c));
					}
					else rowData[colIndex++] = formatValue(baseResult.getStringValue(r, c));
				}
				for (QueryResult res: featureResults) {
					for (int c = 0; c < res.getColumns().length; c++) {
						boolean numeric = res.isColumnNumeric(c);
						if (numeric) rowData[colIndex++] = formatValue(res.getNumericValue(r, c));
						else rowData[colIndex++] = formatValue(res.getStringValue(r, c));
					}
				}
				writer.writeNext(rowData);
			}
		}
		
		if (settings.includes.contains(Includes.PlateStatistics) && !featureResults.isEmpty()) {
			// Write statistics into separate file.
			String extension = FileUtils.getExtension(destination);
			destination = destination.substring(0,destination.lastIndexOf('.'));
			destination += "_Statistics." + extension;
			try (au.com.bytecode.opencsv.CSVWriter writer = new au.com.bytecode.opencsv.CSVWriter(new FileWriter(destination), columnSeparator, quoteChar, escapeChar)) {
				
				Statistics stats = featureResults.get(0).getStatistics();
				List<String> statNames = stats.getStatNames().stream().sorted().collect(Collectors.toList());
				
				String[] rowData = new String[settings.features.size()+1];
				rowData[0] = "";
				for (int c = 0; c < rowData.length-1; c++) {
					rowData[c+1] = settings.features.get(c).getDisplayName();
				}
				writer.writeNext(rowData);
				for (int r = 0; r < statNames.size(); r++) {
					rowData[0] = statNames.get(r);
					for (int c = 0; c < settings.features.size(); c++) {
						stats = featureResults.get(c).getStatistics();
						rowData[c+1] = ""+stats.get(rowData[0]);
					}
					writer.writeNext(rowData);
				}
			}
		}
	}
	
	private String formatValue(double value) {
		if (Double.isNaN(value)) return "";
		return ""+value;
	}
	
	private String formatValue(String value) {
		String formattedValue = value;
		if (value == null) formattedValue = "";
		if (valueConverter != null) formattedValue = valueConverter.convert(value);
		if (needQuoting()) formattedValue = "\"" + formattedValue + "\"";
		return formattedValue;
	}
}
