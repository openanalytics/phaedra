package eu.openanalytics.phaedra.export.core.writer.format;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.ExportSettings.Includes;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.statistics.Statistics;
import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;

public class StreamingXLSXWriter implements IExportWriter {

	private ExportSettings settings;
	private IValueConverter valueConverter;
	
	private QueryResult baseResult;
	private List<QueryResult> featureResults;
	
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
		writeWorkbook();
	}

	@Override
	public void rollback() {
		// Nothing to do.
	}

	/*
	 * Non-public
	 * **********
	 */
	
	private void writeWorkbook() throws IOException {
		SXSSFWorkbook wb = new SXSSFWorkbook(100);
		Sheet sh = wb.createSheet();
		
		// First row: headers
		Row row = sh.createRow(0);
		int colNr = 0;
		for (String header: baseResult.getColumns()) writeCell(row, colNr++, header); 
		for (QueryResult res: featureResults) {
			for (String header: res.getColumns()) writeCell(row, colNr++, header); 
		}
		
		// Other rows: data values
		for (int rowNr = 1; rowNr < baseResult.getRowCount(); rowNr++) {
			row = sh.createRow(rowNr);
			for (colNr = 0; colNr < baseResult.getColumns().length; colNr++) {
				boolean numeric = baseResult.isColumnNumeric(colNr);
				if (numeric) writeCell(row, colNr, baseResult.getNumericValue(rowNr-1, colNr));
				else writeCell(row, colNr, baseResult.getStringValue(rowNr-1, colNr));
			}
			
			int colOffset = colNr;
			for (QueryResult res: featureResults) {
				for (colNr = 0; colNr < res.getColumns().length; colNr++) {
					boolean numeric = res.isColumnNumeric(colNr);
					if (numeric) writeCell(row, colOffset+colNr, res.getNumericValue(rowNr-1, colNr));
					else writeCell(row, colOffset+colNr, res.getStringValue(rowNr-1, colNr));
				}
				colOffset += colNr;
			}
		}
		
		if (settings.includes.contains(Includes.PlateStatistics) && !featureResults.isEmpty()) {
			Statistics stats = featureResults.get(0).getStatistics();
			List<String> statNames = stats.getStatNames().stream().sorted().collect(Collectors.toList());
			
			sh = wb.createSheet("Statistics");
			row = sh.createRow(0);
			for (int c = 0; c < settings.features.size(); c++) {
				writeCell(row, c+1, settings.features.get(c).getDisplayName());
			}
			for (int r = 0; r < statNames.size(); r++) {
				row = sh.createRow(r+1);
				String statName = statNames.get(r);
				writeCell(row, 0, statName);
				for (int c = 0; c < settings.features.size(); c++) {
					stats = featureResults.get(c).getStatistics();
					writeCell(row, c+1, stats.get(statName));
				}
			}
		}
		
		try (OutputStream out = new FileOutputStream(settings.destinationPath)) {
			wb.write(out);
		} finally {
			wb.dispose();
		}
	}
	
	private void writeCell(Row row, int column, String data) {
		Cell cell = row.createCell(column);
		if (valueConverter != null) data = valueConverter.convert(data);
		if (data == null) {
			cell.setCellType(Cell.CELL_TYPE_BLANK);
		} else if (NumberUtils.isDigit(data) || NumberUtils.isDouble(data)) {
			cell.setCellType(Cell.CELL_TYPE_NUMERIC);
			cell.setCellValue(Double.parseDouble(data));
		} else {
			cell.setCellValue(new XSSFRichTextString(data));
		}
	}
	
	private void writeCell(Row row, int column, double data) {
		Cell cell = row.createCell(column);
		if (Double.isNaN(data)) cell.setCellType(Cell.CELL_TYPE_BLANK);
		else {
			cell.setCellType(Cell.CELL_TYPE_NUMERIC);
			cell.setCellValue(data);
		}
	}
}
