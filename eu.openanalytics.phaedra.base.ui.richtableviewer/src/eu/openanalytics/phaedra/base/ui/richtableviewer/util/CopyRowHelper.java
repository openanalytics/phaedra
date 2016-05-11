package eu.openanalytics.phaedra.base.ui.richtableviewer.util;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.management.RuntimeErrorException;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class CopyRowHelper {

	public static void copyToClipboard(Table table, Clipboard clipboard) {
		StringBuilder sb = new StringBuilder();
		TableColumn[] cols = table.getColumns();
		TableItem[] items = table.getSelection();

		if (items.length == table.getItemCount()) {
			// Full selection: include headers.
			for (int c = 0; c < cols.length; c++) {
				if (cols[c].getWidth() > 0) {
					String text = cols[c].getText();
					if (text == null)
						text = "";
					sb.append(text);
					if (c + 1 < cols.length)
						sb.append("\t");
				}
			}
			sb.append("\n");
		}

		for (int i = 0; i < items.length; i++) {
			for (int c = 0; c < cols.length; c++) {
				if (cols[c].getWidth() > 0) {
					String text = items[i].getText(c);
					if (text == null || text.isEmpty()) {
						Object viewerColumn = table.getColumn(c).getData("org.eclipse.jface.columnViewer");
						if (viewerColumn != null) {
							Object labelProvider = ReflectionUtils.invoke("getLabelProvider", viewerColumn);
							if (labelProvider != null) {
								text = (String)ReflectionUtils.invoke("getText", labelProvider, new Object[]{items[i].getData()}, new Class<?>[]{Object.class});
							}
						}
					}
					if (text == null)
						text = "";
					sb.append(text);
					if (c + 1 < cols.length)
						sb.append("\t");
				}
			}
			sb.append("\n");
		}

		String data = sb.toString();
		if (!data.isEmpty()) {
			clipboard.setContents(new Object[] { data },
					new Transfer[] { TextTransfer.getInstance() });
		}
	}
	
	public static void copyToExcel(Table table) {
		FileDialog fd = new FileDialog(new Shell(), SWT.SAVE);
		fd.setText("Save");
		fd.setFilterPath(System.getProperty("user.home"));
		String[] filterExt = { "*.xlsx", "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();

		if (selected != null) {
			TableColumn[] cols = table.getColumns();
			TableItem[] items = table.getSelection();

			XSSFWorkbook wb = new XSSFWorkbook();
			XSSFSheet sheet = wb.createSheet("Sheet1");
			XSSFDrawing draw = sheet.createDrawingPatriarch();

			sheet.createRow(0);
			for (int c = 0; c < cols.length; c++) {
				if (cols[c].getWidth() > 0) {
					String text = cols[c].getText();
					if (text == null)
						text = "";
					sheet.getRow(0).createCell(c).setCellValue(text);
				}
			}

			for (int i = 0; i < items.length; i++) {
				sheet.createRow(i+1);
				for (int c = 0; c < cols.length; c++) {
					if (cols[c].getWidth() > 0) {
						XSSFCell cell = sheet.getRow(i+1).createCell(c);

						if (items[i].getImage(c) == null) {
							String text = items[i].getText(c);
							
							if (text == null || text.isEmpty()) {
								Object viewerColumn = table.getColumn(c).getData("org.eclipse.jface.columnViewer");
								if (viewerColumn != null) {
									Object labelProvider = ReflectionUtils.invoke("getLabelProvider", viewerColumn);
									if (labelProvider != null) {
										text = (String)ReflectionUtils.invoke("getText", labelProvider, new Object[]{items[i].getData()}, new Class<?>[]{Object.class});
									}
								}
							}
							
							if (text == null)
								text = "";
							if (NumberUtils.isDouble(text) && !text.equalsIgnoreCase("nan"))
								cell.setCellValue(Double.parseDouble(text));
							else
								cell.setCellValue(text);
						} else {
							ImageLoader loader = new ImageLoader();
							loader.data = new ImageData[] {items[i].getImage(c).getImageData()};
							ByteArrayOutputStream img_bytes = new ByteArrayOutputStream();
							loader.save(img_bytes, SWT.IMAGE_PNG);
							XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, c, i + 1, c + 1, i + 2);
							anchor.setAnchorType(XSSFClientAnchor.MOVE_AND_RESIZE);
							int	index = wb.addPicture(img_bytes.toByteArray(),XSSFWorkbook.PICTURE_TYPE_PNG);
							draw.createPicture(anchor, index);
						}

						sheet.autoSizeColumn(c);

						// Style
						// TODO: When no Font color is specified for a column, the parent foreground is used which is white.
						org.eclipse.swt.graphics.Color foreground = items[i].getForeground(c);
						org.eclipse.swt.graphics.Color background = items[i].getBackground(c);
						java.awt.Color foregroundColorAWT = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue());
						java.awt.Color backgroundColorAWT = new Color(background.getRed(), background.getGreen(), background.getBlue());
						XSSFFont font = wb.createFont();
						font.setColor(new XSSFColor(foregroundColorAWT));
						XSSFCellStyle cellStyle = wb.createCellStyle();
						if (backgroundColorAWT.getRGB() != -1)
							cellStyle.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
						else
							cellStyle.setFillPattern(XSSFCellStyle.NO_FILL);
						cellStyle.setFillForegroundColor(new XSSFColor(backgroundColorAWT));
						cellStyle.setFont(font);
						cellStyle.setAlignment(HorizontalAlignment.LEFT);
						cell.setCellStyle(cellStyle);
					}
				}
			}

			FileOutputStream out = null;
			try {
				out = new FileOutputStream(selected);
				wb.write(out);
			} catch (IOException e) {
				MessageDialog.openError(new Shell(), "Error", "The Excel file could not be saved.\n\nPlease make sure that the file is closed and try again.");
			} finally {
				if (out != null)
					try {
						out.close();
					} catch (IOException e) {
						throw new RuntimeErrorException(null, e.getMessage());
					}
			}
		}
	}
}
