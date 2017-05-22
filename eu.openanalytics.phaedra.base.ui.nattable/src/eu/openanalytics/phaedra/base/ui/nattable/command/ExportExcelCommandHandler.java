package eu.openanalytics.phaedra.base.ui.nattable.command;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.ClientAnchor.AnchorType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel.ColumnGroup;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.util.misc.ThreadsafeDialogHelper;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;

public class ExportExcelCommandHandler extends AbstractLayerCommandHandler<ExportExcelCommand> {

	private SelectionLayer selectionLayer;
	private ColumnGroupModel columnGroupModel;
	private IColumnPropertyAccessor<?> columnAccessor;
	
	public ExportExcelCommandHandler(SelectionLayer selectionLayer, ColumnGroupModel columnGroupModel, IColumnPropertyAccessor<?> columnAccessor) {
		this.selectionLayer = selectionLayer;
		this.columnGroupModel = columnGroupModel;
		this.columnAccessor = columnAccessor;
	}

	@Override
	public Class<ExportExcelCommand> getCommandClass() {
		return ExportExcelCommand.class;
	}

	@Override
	protected boolean doCommand(ExportExcelCommand cmd) {
		int exportMode = promptExportMode();
		if (exportMode == -1) return true;
		
		String destinationPath = promptFileSelector();
		if (destinationPath == null) return true;
		
		JobUtils.runUserJob(monitor -> {
			try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
				Sheet sheet = wb.createSheet();
				writeData(sheet, exportMode, monitor);
				try (OutputStream out = new FileOutputStream(destinationPath)) {
					wb.write(out);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}, "Exporting table", selectionLayer.getRowCount(), null, null);
		return true;
	}

	private void writeData(Sheet sheet, int exportMode, IProgressMonitor monitor) {
		if (exportMode == 0) {
			Collection<ILayerCell> cells = ((SelectionLayer) selectionLayer).getSelectedCells();
			int rowOffset = cells.stream().mapToInt(c -> c.getRowPosition()).min().orElse(0);
			int colOffset = cells.stream().mapToInt(c -> c.getColumnPosition()).min().orElse(0);
			
			for (ILayerCell cell: cells) {
				int rowNr = cell.getRowPosition() - rowOffset;
				int colNr = cell.getColumnPosition() - colOffset;
				Row row = sheet.getRow(rowNr);
				if (row == null) row = sheet.createRow(rowNr);
				writeCell(row, colNr, cell.getDataValue());
				monitor.worked(1);
			}
		} else {
			Row headerRow = sheet.createRow(0);
			for (int columnPosition = 0; columnPosition < selectionLayer.getColumnCount(); columnPosition++) {
				int colIndex = selectionLayer.getColumnIndexByPosition(columnPosition);
				String header = columnAccessor.getColumnProperty(colIndex);
				ColumnGroup group = columnGroupModel.getColumnGroupByIndex(colIndex);
				if (group != null) header = group.getName() + " " + header;
				writeCell(headerRow, columnPosition, header);
			}
			for (int rowPosition = 0; rowPosition < selectionLayer.getRowCount(); rowPosition++) {
				Row row = sheet.createRow(1 + rowPosition);
				for (int columnPosition = 0; columnPosition < selectionLayer.getColumnCount(); columnPosition++) {
					ILayerCell cell = selectionLayer.getCellByPosition(columnPosition, rowPosition);
					writeCell(row, columnPosition, cell.getDataValue());
				}
				monitor.worked(1);
			}	
		}
	}
	
	private void writeCell(Row row, int column, Object data) {
		Cell cell = row.createCell(column);
		if (data == null) {
			cell.setCellType(CellType.BLANK);
		} else if (data instanceof Number) {
			cell.setCellType(CellType.NUMERIC);
			cell.setCellValue(((Number) data).doubleValue());
		} else if (data instanceof String && NumberUtils.isDouble((String) data)) {
			double numericData = Double.parseDouble((String) data);
			if (Double.isNaN(numericData) || Double.isInfinite(numericData)) cell.setCellValue(new XSSFRichTextString((String) data));
			else {
				cell.setCellType(CellType.NUMERIC);
				cell.setCellValue(numericData);
			}			
		} else if (data instanceof String) {
			cell.setCellValue(new XSSFRichTextString((String) data));
		} else if (data instanceof ImageData) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			ImageLoader il = new ImageLoader();
			il.data = new ImageData[] { (ImageData) data };
			il.save(buffer, SWT.IMAGE_PNG);

			row.getSheet().setColumnWidth(column, 40 * il.data[0].width);
			row.getSheet().setDefaultRowHeight((short) (20 * il.data[0].height));
			
			CreationHelper helper = row.getSheet().getWorkbook().getCreationHelper();
			ClientAnchor anchor = helper.createClientAnchor();
			anchor.setAnchorType(AnchorType.DONT_MOVE_AND_RESIZE);
			anchor.setRow1(row.getRowNum());
			anchor.setCol1(column);
			int pictureIndex = row.getSheet().getWorkbook().addPicture(buffer.toByteArray(), Workbook.PICTURE_TYPE_PNG);
			Drawing drawing = row.getSheet().createDrawingPatriarch();
			Picture picture = drawing.createPicture(anchor, pictureIndex);
			picture.resize();
		}
	}
	
	private String promptFileSelector() {
		Shell shell = Display.getCurrent().getActiveShell();
		
		FileDialog dialog = new FileDialog(shell, SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"xlsx"});
		dialog.setFilterNames(new String[] {"Excel Workbook (*.xlsx)"});
		dialog.setFileName("export");
		dialog.setText("Select a destination for the export");
		String selectedPath = dialog.open();
		if (selectedPath == null) return null;
		
		selectedPath += "." + dialog.getFilterExtensions()[0];
		if (new File(selectedPath).exists()) {
			boolean confirmed = MessageDialog.openConfirm(shell, "File exists",
					"Are you sure you want to overwrite this file?\n" + selectedPath);
			if (!confirmed) {
				return promptFileSelector();
			}
		}
		return selectedPath;
	}
	
	private int promptExportMode() {
		return ThreadsafeDialogHelper.openChoice("Export Table", "What should be exported? ", new String[] {
				"The selected cell(s)",
				"The whole table"
		});
	}
}
