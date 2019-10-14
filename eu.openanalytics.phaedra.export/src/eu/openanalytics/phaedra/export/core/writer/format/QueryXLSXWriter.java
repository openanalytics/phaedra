package eu.openanalytics.phaedra.export.core.writer.format;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.base.datatype.description.RealValueDescription;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.query.QueryResult.Column;
import eu.openanalytics.phaedra.export.core.statistics.Statistics;
import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;


public class QueryXLSXWriter extends AbstractXLSXWriter implements IExportWriter {
	
	
	private QueryResult baseResult;
	private List<QueryResult> featureResults;
	
	
	@Override
	public void initialize(IExportExperimentsSettings settings) {
		super.initialize(settings);
		
		this.featureResults = new ArrayList<>();
	}
	
	@Override
	public void setValueConverter(IValueConverter valueConverter) {
		setNameConverter(valueConverter);
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
		wb = new SXSSFWorkbook(100);
		try {
			writeMainTable();
			
			if (getSettings() instanceof ExportSettings) {
				writeWellsExportAdditions();
			}
			
			writeExportInfo();
			
			try (OutputStream out = new FileOutputStream(getSettings().getDestinationPath())) {
				wb.write(out);
			}
		}
		finally {
			dispose();
		}
	}

	@Override
	public void rollback() {
		// Nothing to do
	}
	

	private void writeMainTable() throws IOException {
		Sheet sh = wb.createSheet("Data");
		
		// First row: headers
		Row row = sh.createRow(0);
		int colNr = 0;
		for (Column column : baseResult.getColumns()) {
			writeNameCell(row, colNr++, column.getName());
		}
		for (QueryResult res: featureResults) {
			for (Column column : res.getColumns()) {
				writeNameCell(row, colNr++, column.getName());
			}
		}
		sh.createFreezePane(0, 1);
		
		// Other rows: data values
		for (int rowNr = 0; rowNr < baseResult.getRowCount(); rowNr++) {
			row = sh.createRow(rowNr + 1);
			colNr = 0;
			for (Column column : baseResult.getColumns()) {
				writeDataCell(row, colNr++, column.getDataType(), baseResult.getValue(rowNr, column.getIndex()));
			}
			
			for (QueryResult res: featureResults) {
				for (Column column : res.getColumns()) {
					writeDataCell(row, colNr++, column.getDataType(), res.getValue(rowNr, column.getIndex()));
				}
			}
		}
	}
	
	private void writeWellsExportAdditions() {
		ExportSettings settings = (ExportSettings)getSettings();
		if (settings.includes.contains(ExportSettings.Includes.PlateStatistics)
				&& !featureResults.isEmpty()) {
			Statistics stats = featureResults.get(0).getStatistics();
			List<String> statNames = stats.getStatNames().stream().sorted().collect(Collectors.toList());
			
			Sheet sh = wb.createSheet("Statistics");
			Row row = sh.createRow(0);
			for (int c = 0; c < settings.features.size(); c++) {
				writeNameCell(row, c+1, settings.features.get(c).getDisplayName());
			}
			sh.createFreezePane(0, 1);
			DataDescription statDescription = new RealValueDescription("Statistic");
			for (int r = 0; r < statNames.size(); r++) {
				row = sh.createRow(r+1);
				String statName = statNames.get(r);
				writeNameCell(row, 0, statName);
				for (int c = 0; c < settings.features.size(); c++) {
					stats = featureResults.get(c).getStatistics();
					writeDataCell(row, c + 1, statDescription, stats.get(statName));
				}
			}
		}
	}
	
}
