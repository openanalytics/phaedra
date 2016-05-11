package eu.openanalytics.phaedra.export.core.subwell;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class SubWellDataWriterFactory {

	public static void write(List<Well> wells, ExportSettings settings) {
		// Filter out Wells according to selection.
		List<Well> filteredWells = new ArrayList<>();
		if (settings.getIncludedWellTypes() == IExportWriter.WELLTYPE_CONTROL) {
			for (Well w : wells) {
				if (settings.isIncludeRejected() || w.getStatus() > -1) {
					if (PlateUtils.isControl(w)) {
						filteredWells.add(w);
					}
				}
			}
		} else if (settings.getIncludedWellTypes() == IExportWriter.WELLTYPE_SAMPLE) {
			for (Well w : wells) {
				if (settings.isIncludeRejected() || w.getStatus() > -1) {
					if (PlateUtils.isSample(w)) {
						filteredWells.add(w);
					}
				}
			}
		} else {
			for (Well w : wells) {
				if (settings.isIncludeRejected() || w.getStatus() > -1) {
					filteredWells.add(w);
				}
			}
		}

		if (!filteredWells.isEmpty()) {
			// Depending on the filetype, an Excel file or CSV will be created.
			if (settings.getFileLocation().toLowerCase().endsWith("csv")) {
				SubWellDataCSVWriter.getInstance().write(filteredWells, settings);
			} else if (settings.getFileLocation().toLowerCase().endsWith("h5")) {
				SubWellDataH5Writer.getInstance().write(filteredWells, settings);
			} else {
				SubWellDataXLSXWriter.getInstance().write(filteredWells, settings);
			}
		}
	}

}