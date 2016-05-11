package eu.openanalytics.phaedra.export.core.subwell;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.export.Activator;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class SubWellDataH5Writer implements IExportWriter {

	private static SubWellDataH5Writer instance = new SubWellDataH5Writer();

	private HashMap<Integer, Object[]> rowMap;

	public static SubWellDataH5Writer getInstance() {
		return instance;
	}

	public void write(final List<Well> wells, final ExportSettings settings) {
		Job job = new Job("Export Subwell Data") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Collections.sort(wells, PlateUtils.WELL_NR_SORTER);
				writeFileSinglePage(wells, settings, monitor);
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	private void writeFileSinglePage(List<Well> wells, ExportSettings settings, IProgressMonitor monitor) {
		monitor.beginTask("Exporting Subwell Data", wells.size());
		
		try (HDF5File file = new HDF5File(settings.getFileLocation(), false)) {
			List<SubWellFeature> features = settings.getSelectedFeatures();
			Double subsetProc = settings.getSubsetProc();

			int colums = features.size() + 5;
			// Write column Headers
			Object[] columnTypes = new Object[colums];
			String[] columnHeaders = new String[colums];
			int col = 0;
			int row = 0;
			columnTypes[col] = Long.MAX_VALUE;
			columnHeaders[col++] = "Plate ID";
			columnTypes[col] = Long.MAX_VALUE;
			columnHeaders[col++] = "Well ID";
			columnTypes[col] = Long.MAX_VALUE;
			columnHeaders[col++] = "Index";
			columnTypes[col] = new String("Striiiiiiiiiiiiiiiiing");
			columnHeaders[col++] = "Comp. Nr";
			columnTypes[col] = Double.NaN;
			columnHeaders[col++] = "Conc.";
			for (SubWellFeature f : features) {
				if (f.isNumeric()) columnTypes[col] = Double.NaN;
				else columnTypes[col] = new String();
				columnHeaders[col++] = f.getDisplayName();
			}

			Random rnd = new Random();
			// Initialize row map
			rowMap = new HashMap<>();
			for (Well w : wells) {
				String coord = PlateUtils.getWellCoordinate(w);
				monitor.subTask("Exporting Well " + coord);
				col = colums - features.size();
				int rowMax = 0;
				for (SubWellFeature f : features) {
					if (monitor.isCanceled()) return;

					row = 0;
					if (f.isNumeric()) {
						float[] data = SubWellService.getInstance().getNumericData(w, f);
						if (data != null) {
							long index = 0;
							for (float d : data) {
								Object[] rowData = getRow(colums, row++, w);
								rowData[2] = index++;
								rowData[col] = d;
							}
							SubWellService.getInstance().removeFromCache(w, f);
						}
					} else {
						String[] data = SubWellService.getInstance().getStringData(w, f);
						if (data != null) {
							for (String d : data) {
								Object[] rowData = getRow(colums, row++, w);
								rowData[col] = d;
							}
							SubWellService.getInstance().removeFromCache(w, f);
						}
					}
					rowMax = Math.max(rowMax, row);
					col++;
				}

				Object[][] o = new Object[rowMax][];
				for (int i = 0; i < rowMax; i++) {
					o[i] = new Object[colums];
				}

				int index = 0;
				for (int i = 0; i < rowMax; i++) {
					if (subsetProc == null || rnd.nextDouble() < subsetProc) {
						Object[] objects = rowMap.get(i);
						
						// TODO: Remove this HARDCODED check for AR Cell Classes.
						if (features.get(0).getDisplayName().equalsIgnoreCase("cell classes")) {
							if (objects == null || objects.length < 5 || objects[5] == null || Float.isNaN(new Float((float) objects[5])) || new Float((float) objects[5]) < 1) {
								continue;
							}
						}
						
						for (col = 0; col < colums; col++) {
							Object object = objects[col];
							if (object != null) {
								o[index][col] = object;
							} else {
								o[index][col] = columnTypes[col];
							}
						}
						index++;
					}
				}
				Object[][] data = Arrays.copyOf(o, index);

				file.writeCompoundData("/DataExport", columnHeaders, columnTypes, data);
				rowMap.clear();

				monitor.worked(1);
			}
		} catch (IOException e) {
			EclipseLog.error("Subwell data H5 export failed", e, Activator.getDefault());
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


	private Object[] getRow(int size, Integer rowIndex, Well w) {
		if (rowMap.containsKey(rowIndex)) {
			return rowMap.get(rowIndex);
		} else {
			Object[] row = new Object[size];
			row[0] = w.getPlate().getId();
			row[1] = w.getId();
			row[2] = PlateUtils.getWellCoordinate(w);
			row[3] = w.getCompound() != null ? w.getCompound().getNumber() : w.getWellType();
			row[4] = w.getCompoundConcentration();
			rowMap.put(rowIndex, row);
			return row;
		}
	}

}