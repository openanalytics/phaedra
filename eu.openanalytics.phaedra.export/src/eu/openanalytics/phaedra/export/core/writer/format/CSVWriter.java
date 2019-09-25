package eu.openanalytics.phaedra.export.core.writer.format;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.statistics.Statistics;
import eu.openanalytics.phaedra.export.core.util.ExportInfo;
import eu.openanalytics.phaedra.export.core.util.ExportInfo.Info;
import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class CSVWriter implements IExportWriter {
	
	private char columnSeparator;
	private char quoteChar;
	private char escapeChar;
	
	private Date timestamp;
	private IExportExperimentsSettings settings;
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
	public void initialize(IExportExperimentsSettings settings) throws IOException {
		this.settings = settings;
		this.timestamp = new Date();
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
		try {
			writeMainTable();
			
			try (au.com.bytecode.opencsv.CSVWriter writer = new au.com.bytecode.opencsv.CSVWriter(
					new FileWriter(getDestinationPath(settings.getDestinationPath(), "Info")), columnSeparator, quoteChar, escapeChar)) {
				writeExportInfo(settings.getExperiments(), timestamp, writer);
			}
			
			if (settings instanceof ExportSettings) {
				writeWellsExportAdditions();
			}
		}
		finally {
		}
	}
	
	@Override
	public void rollback() {
		// Nothing to do
	}
	
	public static void writeExportInfo(List<Experiment> experiments, Date timestamp, au.com.bytecode.opencsv.CSVWriter writer) throws IOException {
		Info[] infos = ExportInfo.get(experiments, timestamp);
		int maxValueSize = Arrays.stream(infos).mapToInt(i -> i.values.size()).max().orElse(1);
		String[] rowData = new String[1 + maxValueSize];
		for (int i = 0; i < infos.length; i++) {
			Arrays.fill(rowData, null);
			Info info = infos[i];
			rowData[0] = info.name;
			for (int valueIdx = 0; valueIdx < info.values.size(); valueIdx++) {
				rowData[1 + valueIdx] = String.valueOf(info.values.get(valueIdx));
			}
			writer.writeNext(rowData);
		}
	}
	
	public static String getDestinationPath(String destinationPath, String sub) {
		String extension = FileUtils.getExtension(destinationPath);
		destinationPath = destinationPath.substring(0, destinationPath.lastIndexOf('.'));
		return destinationPath + '_' + sub + '.' + extension;
	}
	
	protected boolean needQuoting() {
		return true;
	}
	
	private void writeMainTable() throws IOException {
		try (au.com.bytecode.opencsv.CSVWriter writer = new au.com.bytecode.opencsv.CSVWriter(new FileWriter(settings.getDestinationPath()),
				columnSeparator, quoteChar, escapeChar)) {
			int colCount = baseResult.getColumnNames().length + featureResults.stream().mapToInt(qr -> qr.getColumnNames().length).sum();
			int colIndex = 0;
			String[] rowData = new String[colCount];
			for (int c = 0; c < baseResult.getColumnNames().length; c++) rowData[colIndex++] = baseResult.getColumnNames()[c];
			for (QueryResult res: featureResults) {
				for (int c = 0; c < res.getColumnNames().length; c++) rowData[colIndex++] = res.getColumnNames()[c];
			}
			writer.writeNext(rowData);
			
			for (int r = 0; r < baseResult.getRowCount(); r++) {
				colIndex = 0;
				for (int c = 0; c < baseResult.getColumnNames().length; c++) {
					boolean numeric = baseResult.isColumnNumeric(c);
					if (numeric) {
						if (baseResult.getColumnNames()[c].endsWith("_ID")) rowData[colIndex++] = "" + Double.valueOf(baseResult.getNumericValue(r, c)).longValue();
						else rowData[colIndex++] = formatValue(baseResult.getNumericValue(r, c));
					}
					else rowData[colIndex++] = formatValue(baseResult.getStringValue(r, c));
				}
				for (QueryResult res: featureResults) {
					for (int c = 0; c < res.getColumnNames().length; c++) {
						boolean numeric = res.isColumnNumeric(c);
						if (numeric) rowData[colIndex++] = formatValue(res.getNumericValue(r, c));
						else rowData[colIndex++] = formatValue(res.getStringValue(r, c));
					}
				}
				writer.writeNext(rowData);
			}
		}
	}
	
	private void writeWellsExportAdditions() throws IOException {
		ExportSettings settings = (ExportSettings) this.settings;
		if (settings.includes.contains(ExportSettings.Includes.PlateStatistics)
				&& !featureResults.isEmpty()) {
			// Write statistics into separate file.
			try (au.com.bytecode.opencsv.CSVWriter writer = new au.com.bytecode.opencsv.CSVWriter(
					new FileWriter(getDestinationPath(settings.getDestinationPath(), "Statistics")), columnSeparator, quoteChar, escapeChar)) {
				
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
		return Double.toString(value);
	}
	
	private String formatValue(String value) {
		String formattedValue = value;
		if (value == null) formattedValue = "";
		if (valueConverter != null) formattedValue = valueConverter.convert(value);
		if (needQuoting()) formattedValue = "\"" + formattedValue + "\"";
		return formattedValue;
	}
	
}
