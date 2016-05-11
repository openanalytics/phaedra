package eu.openanalytics.phaedra.export.core.subwell;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.export.Activator;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class SubWellDataXLSXWriter implements IExportWriter {

	private static SubWellDataXLSXWriter instance = new SubWellDataXLSXWriter();

	private Random random;

	private SubWellDataXLSXWriter() {
		random = new Random();
	}

	public static SubWellDataXLSXWriter getInstance() {
		return instance;
	}

	public void write(final List<Well> wells, final ExportSettings settings) {
		Job job = new Job("Export Subwell Data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Collections.sort(wells, PlateUtils.WELL_NR_SORTER);
				SXSSFWorkbook wb = null;
				try {
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 70);
					if (settings.getExportMode() == MODE_PAGE_PER_WELL) {
						wb = fillFilePagePerWell(wells, settings, subMonitor);
					} else {
						wb = fillFileSinglePage(wells, settings, subMonitor);
					}

					if (monitor.isCanceled()) return Status.CANCEL_STATUS;

					// Write the Excel file to the given location.
					subMonitor = new SubProgressMonitor(monitor, 30);
					writeFile(settings.getFileLocation(), wb, subMonitor);
				} finally {
					if (wb != null) wb.dispose();
				}
				return Status.OK_STATUS;
			}
		};

		job.setUser(true);
		job.schedule();
	}

	private SXSSFWorkbook fillFileSinglePage(List<Well> wells, ExportSettings settings, IProgressMonitor monitor) {
		monitor.beginTask("Exporting Subwell Data", wells.size());
		List<SubWellFeature> features = settings.getSelectedFeatures();
		Double subsetProc = settings.getSubsetProc();

		// We will manually flush rows.
		SXSSFWorkbook wb = new SXSSFWorkbook(-1);
		SXSSFSheet sheet = (SXSSFSheet) wb.createSheet("Subwell Data");

		int rowStart = 0;
		int rowEnd = 1;
		int rowIndex = 0;
		int colIndex = 0;
		
		// Header row for the Feature names.
		Row headerRow = sheet.createRow(rowIndex++);
		headerRow.createCell(colIndex++).setCellValue("Plate ID");
		headerRow.createCell(colIndex++).setCellValue("Well ID");
		headerRow.createCell(colIndex++).setCellValue("Well Nr.");
		headerRow.createCell(colIndex++).setCellValue("Comp. Nr");
		headerRow.createCell(colIndex++).setCellValue("Conc.");
		for (SubWellFeature f : features) headerRow.createCell(colIndex++).setCellValue(f.getDisplayName());

		for (Well w : wells) {
			String coord = PlateUtils.getWellCoordinate(w);
			monitor.subTask("Exporting Well " + coord);

			rowStart = rowEnd;
			colIndex = 5;
			boolean[] subsetFilter = null;
			
			for (SubWellFeature f : features) {
				if (monitor.isCanceled()) return wb;

				rowIndex = rowStart;
				if (f.isNumeric()) {
					float[] data = SubWellService.getInstance().getNumericData(w, f);
					if (data != null) {
						if (subsetFilter == null) subsetFilter = createSubsetFilter(data.length, subsetProc);
						for (int i = 0; i < data.length; i++) {
							if (subsetFilter == null || subsetFilter[i]) {
								rowIndex = updateCell(sheet, colIndex, rowIndex, data[i], w, true);
							}
						}
					}
				} else {
					String[] data = SubWellService.getInstance().getStringData(w, f);
					if (data != null) {
						if (subsetFilter == null) subsetFilter = createSubsetFilter(data.length, subsetProc);
						for (int i = 0; i < data.length; i++) {
							if (subsetFilter == null || subsetFilter[i]) {
								rowIndex = updateCell(sheet, colIndex, rowIndex, data[i], w, true);
							}
						}
					}
				}

				colIndex++;
				rowEnd = Math.max(rowEnd, rowIndex);
			}

			try {
				// Here we flush the rows after each Well.
				sheet.flushRows();
			} catch (IOException e) {
				Activator.getDefault().getLog().log(new Status(Status.WARNING, Activator.PLUGIN_ID, e.getMessage()));
			}

			monitor.worked(1);
		}

		monitor.done();
		return wb;
	}
	
	private SXSSFWorkbook fillFilePagePerWell(List<Well> wells, ExportSettings settings, IProgressMonitor monitor) {
		monitor.beginTask("Exporting Subwell Data", wells.size());
		SXSSFWorkbook wb = new SXSSFWorkbook(-1);

		List<SubWellFeature> features = settings.getSelectedFeatures();
		Double subsetProc = settings.getSubsetProc();

		for (Well w : wells) {
			String wellCoord = PlateUtils.getWellCoordinate(w);
			monitor.subTask("Exporting Well " + wellCoord);

			// Create a new sheet for each well.
			SXSSFSheet sheet = (SXSSFSheet) wb.createSheet(wellCoord);
			int colIndex = 0;
			boolean[] subsetFilter = null;
			
			// Header row for the Feature names.
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(colIndex++).setCellValue("Plate ID");
			headerRow.createCell(colIndex++).setCellValue("Well ID");
			headerRow.createCell(colIndex++).setCellValue("Comp. Nr");
			headerRow.createCell(colIndex++).setCellValue("Conc.");
			
			// For each Feature, create a column.
			for (SubWellFeature f : features) {
				if (monitor.isCanceled()) return wb;

				headerRow.createCell(colIndex).setCellValue(f.getDisplayName());
				int rowIndex = 1;

				if (f.isNumeric()) {
					float[] data = SubWellService.getInstance().getNumericData(w, f);
					if (data != null) {
						if (subsetFilter == null) subsetFilter = createSubsetFilter(data.length, subsetProc);
						for (int i = 0; i < data.length; i++) {
							if (subsetFilter == null || subsetFilter[i]) {
								rowIndex = updateCell(sheet, colIndex, rowIndex, data[i], w, false);	
							}
						}
					}
				} else {
					String[] data = SubWellService.getInstance().getStringData(w, f);
					if (data != null) {
						if (subsetFilter == null) subsetFilter = createSubsetFilter(data.length, subsetProc);
						for (int i = 0; i < data.length; i++) {
							if (subsetFilter == null || subsetFilter[i]) {
								rowIndex = updateCell(sheet, colIndex, rowIndex, data[i], w, false);	
							}
						}
					}
				}

				colIndex++;
			}

			try {
				// Here we flush the rows after each Well.
				sheet.flushRows();
			} catch (IOException e) {
				Activator.getDefault().getLog().log(new Status(Status.WARNING, Activator.PLUGIN_ID, e.getMessage()));
			}

			monitor.worked(1);
		}

		monitor.done();
		return wb;
	}

	private boolean[] createSubsetFilter(int dataSize, Double subsetPercentage) {
		if (subsetPercentage == null || subsetPercentage == 1.0) return null;
		boolean[] filter = new boolean[dataSize];
		for (int i = 0; i < filter.length; i++) {
			filter[i] = random.nextDouble() < subsetPercentage;
		}
		return filter;
	}
	
	private int updateCell(Sheet sheet, int colIndex, int rowIndex, Object value, Well w, boolean hasWellNr) {
		Row row = sheet.getRow(rowIndex);
		if (row == null) {
			row = sheet.createRow(rowIndex);
			int index = 0;
			row.createCell(index++).setCellValue(w.getPlate().getId());
			row.createCell(index++).setCellValue(w.getId());
			if (hasWellNr) row.createCell(index++).setCellValue(PlateUtils.getWellCoordinate(w));
			row.createCell(index++).setCellValue(w.getCompound() != null ? w.getCompound().getNumber() : w.getWellType());
			row.createCell(index++).setCellValue(w.getCompoundConcentration() + "");
		}

		Cell cell = row.createCell(colIndex);
		if (value instanceof Number) {
			cell.setCellValue(((Number) value).doubleValue());
		} else {
			cell.setCellValue((String) value);
		}
		
		return rowIndex + 1;
	}

	private static void writeFile(String fileLocation, Workbook wb, IProgressMonitor monitor) {
		monitor.beginTask("Finalizing Task", 20);
		File file = new File(fileLocation);
		if (file.getParentFile() != null) file.getParentFile().mkdirs();
		try (FileOutputStream out = new FileOutputStream(file)) {
			monitor.subTask("Writing File...");
			wb.write(out);
			monitor.worked(10);

			Program p = Program.findProgram(fileLocation);
			if (p != null) {
				monitor.subTask("Opening File...");
				p.execute(fileLocation);
			}
			monitor.worked(10);
		} catch (IOException e) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Export Failed!"
							, "Could not write the file to the given location.\n"
									+ "Please make sure that you have sufficient permissions and that the file is not in use.");
				}
			});
		}
		monitor.done();
	}

}