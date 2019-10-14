package eu.openanalytics.phaedra.export.core.subwell;

import static eu.openanalytics.phaedra.base.datatype.unit.ConcentrationUnit.Molar;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import au.com.bytecode.opencsv.CSVWriter;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.export.core.IExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.writer.format.AbstractCSVWriter;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;


public class SubWellDataCSVWriter extends AbstractCSVWriter implements IExportWriter {
	
	
	private Map<Integer, String[]> rowMap;
	
	
	private DataFormatter dataFormatter;
	
	
	public SubWellDataCSVWriter() {
	}
	
	
	@Override
	public void initialize(final IExportExperimentsSettings settings, final DataFormatter dataFormatter) {
		super.initialize(settings);
		
		this.dataFormatter = dataFormatter;
	}
	
	@Override
	public void write(final List<Well> wells, final ExportSettings settings) {
		Job job = new Job("Export Subwell Data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Collections.sort(wells, PlateUtils.WELL_NR_SORTER);

				writeFileSinglePage(wells, settings, monitor);

				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}

	private void writeFileSinglePage(List<Well> wells, ExportSettings settings, IProgressMonitor monitor) {

		monitor.beginTask("Exporting Subwell Data", wells.size());
		try (CSVWriter writer = new CSVWriter(new FileWriter(settings.getFileLocation()), ',')) {
			List<SubWellFeature> features = settings.getSelectedFeatures();
			Double subsetProc = settings.getSubsetProc();

			int colums = features.size() + 6;

			// Write column Headers
			String[] columnHeaders = new String[colums];
			int col = 0;
			int row = 1;
			columnHeaders[col++] = "Plate ID";
			columnHeaders[col++] = "Well ID";
			columnHeaders[col++] = "Well Nr";
			columnHeaders[col++] = "Index";
			columnHeaders[col++] = "Comp. Nr";
			columnHeaders[col++] = "Conc.";
			for (SubWellFeature f : features) {
				columnHeaders[col++] = f.getDisplayName();
			}
			writer.writeNext(columnHeaders);

			Random rnd = new Random();
			// Initialize row map
			rowMap = new HashMap<>();
			int rowStart = row;
			int rowEnd = row;
			for (Well w : wells) {
				String coord = PlateUtils.getWellCoordinate(w);
				monitor.subTask("Exporting Well " + coord);
				col = colums - features.size();
				rowStart = rowEnd;
				for (SubWellFeature f : features) {

					if (monitor.isCanceled()) {
						return;
					}

					row = rowStart;
					if (f.isNumeric()) {
						float[] data = SubWellService.getInstance().getNumericData(w, f);
						if (data != null) {
							for (int i = 0; i < data.length; i++) {
								String[] rowData = getRow(colums, row++, w, i);
								rowData[col] = data[i] + "";
							}
						}
					} else {
						String[] data = SubWellService.getInstance().getStringData(w, f);
						if (data != null) {
							for (int i = 0; i < data.length; i++) {
								String[] rowData = getRow(colums, row++, w, i);
								rowData[col] = data[i];
							}
						}
					}
					col++;
					rowEnd = Math.max(row, rowEnd);
				}

				for (int i = rowStart; i < rowEnd; i++) {
					if (subsetProc == null || rnd.nextDouble() < subsetProc) {
						String[] data = rowMap.get(i);
						writer.writeNext(data);
					}
				}
				rowMap = new HashMap<>();

				monitor.worked(1);
			}
			
			try (CSVWriter infoWriter = new CSVWriter(
					new FileWriter(getDestinationPath(settings.getFileLocation(), "Info")), ',')) {
				writeExportInfo(infoWriter);
			}
			
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
	}

	private String[] getRow(int size, Integer rowIndex, Well w, int index) {
		if (rowMap.containsKey(rowIndex)) {
			return rowMap.get(rowIndex);
		} else {
			String[] row = new String[size];
			row[0] = w.getPlate().getId() + "";
			row[1] = w.getId() + "";
			row[2] = PlateUtils.getWellCoordinate(w);
			row[3] = index + "";
			row[4] = w.getCompound() != null ? w.getCompound().getNumber() : w.getWellType();
			row[5] = Double.toString(this.dataFormatter.getConcentrationUnit()
					.convert(w.getCompoundConcentration(), Molar) );
			rowMap.put(rowIndex, row);
			return row;
		}
	}

}
