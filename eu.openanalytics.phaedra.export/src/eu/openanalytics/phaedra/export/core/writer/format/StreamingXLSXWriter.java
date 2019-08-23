package eu.openanalytics.phaedra.export.core.writer.format;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.export.core.BaseExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.ExportSettings;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.query.QueryResult;
import eu.openanalytics.phaedra.export.core.statistics.Statistics;
import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

public class StreamingXLSXWriter implements IExportWriter {
	
	
	private static class Info {
		
		String name;
		byte valueType;
		List<?> values;
		
		public Info(String name, byte valueType, List<?> values) {
			this.name= name;
			this.valueType = valueType;
			this.values = values;
		}
		
		public Info(String name, byte valueType, Object value) {
			this.name= name;
			this.valueType = valueType;
			this.values = Collections.singletonList(value);
		}
		
	}
	
	/** For SubWellDataXLSXWriter */
	public static void writeExportInfo(Collection<Experiment> experiments, Date timestamp, SXSSFWorkbook wb) {
		StreamingXLSXWriter writer = new StreamingXLSXWriter();
		writer.initialize(new BaseExportExperimentsSettings(new ArrayList<>(experiments)));
		writer.wb = wb;
		if (timestamp != null) writer.timestamp = timestamp;
		writer.writeExportInfo();
	}
	
	
	private Date timestamp;
	private Protocol protocol;
	
	private IExportExperimentsSettings settings;
	private IValueConverter valueConverter;
	
	private QueryResult baseResult;
	private List<QueryResult> featureResults;
	
	private SXSSFWorkbook wb;
	private CellStyle dateCellStyle;
	
	@Override
	public void initialize(IExportExperimentsSettings settings) {
		this.settings = settings;
		this.featureResults = new ArrayList<>();
		
		this.timestamp = new Date();
		this.protocol = settings.getExperiments().get(0).getProtocol();
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
		wb = new SXSSFWorkbook(100);
		try {
			writeMainTable();
			
			if (settings instanceof ExportSettings) {
				writeWellsExportAdditions();
			}
			
			writeExportInfo();
			
			try (OutputStream out = new FileOutputStream(settings.getDestinationPath())) {
				wb.write(out);
			}
		}
		finally {
			wb.dispose();
			wb = null;
			dateCellStyle = null;
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
		for (String header: baseResult.getColumnNames()) writeNameCell(row, colNr++, header);
		for (QueryResult res: featureResults) {
			for (String header: res.getColumnNames()) writeNameCell(row, colNr++, header);
		}
		sh.createFreezePane(0, 1);
		
		// Other rows: data values
		for (int rowNr = 0; rowNr < baseResult.getRowCount(); rowNr++) {
			row = sh.createRow(rowNr + 1);
			for (colNr = 0; colNr < baseResult.getColumnNames().length; colNr++) {
				writeDataCell(row, colNr, baseResult.getColumnValueType(colNr), baseResult.getValue(rowNr, colNr));
			}
			
			int colOffset = colNr;
			for (QueryResult res: featureResults) {
				for (colNr = 0; colNr < res.getColumnNames().length; colNr++) {
					writeDataCell(row, colOffset+colNr, res.getColumnValueType(colNr), res.getValue(rowNr, colNr));
				}
				colOffset += colNr;
			}
		}
	}
	
	private void writeWellsExportAdditions() {
		ExportSettings settings = (ExportSettings) this.settings;
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
			for (int r = 0; r < statNames.size(); r++) {
				row = sh.createRow(r+1);
				String statName = statNames.get(r);
				writeNameCell(row, 0, statName);
				for (int c = 0; c < settings.features.size(); c++) {
					stats = featureResults.get(c).getStatistics();
					writeDataCell(row, c+1, QueryResult.DOUBLE_VALUE, stats.get(statName));
				}
			}
		}
	}
	
	
	private void writeExportInfo() {
		Info[] infos = new Info[] {
				new Info("Protocol ID", QueryResult.DOUBLE_VALUE, (double)protocol.getId()),
				new Info("Protocol Name", QueryResult.STRING_VALUE, protocol.getName()),
				new Info("Protocol Class ID", QueryResult.DOUBLE_VALUE, (double)protocol.getProtocolClass().getId()),
				new Info("Protocol Class Name", QueryResult.STRING_VALUE, protocol.getProtocolClass().getName()),
				new Info("Experiment ID", QueryResult.DOUBLE_VALUE,
						settings.getExperiments().stream().map((experiment) -> (double)experiment.getId()).collect(Collectors.toList())), 
				new Info("Experiment Name", QueryResult.STRING_VALUE,
						settings.getExperiments().stream().map((experiment) -> experiment.getName()).collect(Collectors.toList())), 
				new Info("Export User", QueryResult.STRING_VALUE, SecurityService.getInstance().getCurrentUserName()),
				new Info("Export Timestamp", QueryResult.TIMESTAMP_VALUE, this.timestamp) };
		
		Sheet sheet = wb.createSheet("Info");
		for (int i = 0; i < infos.length; i++) {
			Info info = infos[i];
			Row row = sheet.createRow(i);
			writeNameCell(row, 0, info.name);
			for (int valueIdx = 0; valueIdx < info.values.size(); valueIdx++) {
				writeDataCell(row, 1 + valueIdx, info.valueType, info.values.get(valueIdx));
			}
		}
		sheet.createFreezePane(1, 0);
	}
	
	
	private void writeNameCell(Row row, int colIdx, String name) {
		Cell cell = row.createCell(colIdx);
		if (valueConverter != null) name = valueConverter.convert(name);
		if (name == null) {
			cell.setCellType(CellType.BLANK);
		} else {
			cell.setCellValue(new XSSFRichTextString(name));
		}
	}
	
	private void writeDataCell(Row row, int colIdx, byte valueType, Object data) {
		Cell cell = row.createCell(colIdx);
		if (data == null) {
			cell.setCellType(CellType.BLANK);
			return;
		}
		switch (valueType) {
		case QueryResult.BOOLEAN_VALUE:
			cell.setCellType(CellType.BOOLEAN);
			cell.setCellValue((Boolean)data);
			return;
		case QueryResult.DOUBLE_VALUE:
			Double d = (Double)data;
			if (d.isNaN()) {
				cell.setCellType(CellType.BLANK);
			}
			else {
				cell.setCellType(CellType.NUMERIC);
				cell.setCellValue((Double)data);
			}
			return;
		case QueryResult.TIMESTAMP_VALUE:
			cell.setCellType(CellType.NUMERIC);
			cell.setCellValue((Date)data);
			cell.setCellStyle(getDateCellStyle());
			return;
		case QueryResult.STRING_VALUE:
			cell.setCellType(CellType.STRING);
			cell.setCellValue(new XSSFRichTextString((String) data));
			return;
		default:
			cell.setCellType(CellType.STRING);
			cell.setCellValue(new XSSFRichTextString(data.toString()));
			return;
		}
	}
	
	private CellStyle getDateCellStyle() {
		if (dateCellStyle == null) {
			dateCellStyle = wb.createCellStyle();
			dateCellStyle.setDataFormat((short)0x16);
		}
		return dateCellStyle;
	}
	
}
