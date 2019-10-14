package eu.openanalytics.phaedra.export.core.writer.format;

import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import eu.openanalytics.phaedra.base.datatype.description.DataDescription;
import eu.openanalytics.phaedra.export.core.ExportInfo;
import eu.openanalytics.phaedra.export.core.writer.convert.IValueConverter;


public class AbstractXLSXWriter extends AbstractExportWriter {
	
	
	protected SXSSFWorkbook wb;
	
	private CellStyle dateCellStyle;
	
	
	protected void dispose() {
		this.dateCellStyle = null;
		if (this.wb != null) {
			this.wb.dispose();
			this.wb = null;
		}
	}
	
	
	protected void writeExportInfo() {
		final List<ExportInfo> infos = getExportInfos();
		
		final Sheet sheet = this.wb.createSheet("Info");
		for (int i = 0; i < infos.size(); i++) {
			final ExportInfo info = infos.get(i);
			final Row row = sheet.createRow(i);
			writeNameCell(row, 0, info.getName());
			final List<?> values = info.getValues();
			for (int valueIdx = 0; valueIdx < values.size(); valueIdx++) {
				writeDataCell(row, 1 + valueIdx, info.getDataType(), values.get(valueIdx));
			}
		}
		sheet.createFreezePane(1, 0);
	}
	
	
	protected void writeNameCell(final Row row, final int colIdx, String name) {
		final Cell cell = row.createCell(colIdx);
		final IValueConverter nameConverter = getNameConverter();
		if (nameConverter != null) {
			name = nameConverter.convert(name);
		}
		if (name == null) {
			cell.setCellType(CellType.BLANK);
		} else {
			cell.setCellValue(new XSSFRichTextString(name));
		}
	}
	
	protected void writeDataCell(final Row row, final int colIdx, final DataDescription dataDescription, final Object data) {
		final Cell cell = row.createCell(colIdx);
		if (data == null) {
			cell.setCellType(CellType.BLANK);
			return;
		}
		switch (dataDescription.getDataType()) {
		case Boolean:
			cell.setCellType(CellType.BOOLEAN);
			cell.setCellValue((Boolean)data);
			return;
		case Integer:
			cell.setCellType(CellType.NUMERIC);
			cell.setCellValue(((Number)data).doubleValue());
			return;
		case Real:
			final double v = ((Number)data).doubleValue();
			if (Double.isNaN(v)) {
				cell.setCellType(CellType.BLANK);
			}
			else if (Double.isInfinite(v)) {
				cell.setCellType(CellType.ERROR);
				cell.setCellErrorValue(FormulaError.NUM.getCode());
			}
			else {
				cell.setCellType(CellType.NUMERIC);
				cell.setCellValue(v);
			}
			return;
		case DateTime:
			cell.setCellType(CellType.NUMERIC);
			cell.setCellValue((Date)data);
			cell.setCellStyle(getDateCellStyle());
			return;
		case String:
			cell.setCellType(CellType.STRING);
			cell.setCellValue(new XSSFRichTextString((String) data));
			return;
		default:
			cell.setCellType(CellType.STRING);
			cell.setCellValue(new XSSFRichTextString(data.toString()));
			return;
		}
	}
	
	protected CellStyle getDateCellStyle() {
		if (this.dateCellStyle == null) {
			this.dateCellStyle = this.wb.createCellStyle();
			this.dateCellStyle.setDataFormat((short)0x16);
		}
		return this.dateCellStyle;
	}
	
}
