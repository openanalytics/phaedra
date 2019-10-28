package eu.openanalytics.phaedra.export.core.subwell;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.datatype.DataTypePrefs;
import eu.openanalytics.phaedra.base.datatype.description.StringValueDescription;
import eu.openanalytics.phaedra.base.datatype.format.DataFormatter;
import eu.openanalytics.phaedra.export.core.BaseExportExperimentsSettings;
import eu.openanalytics.phaedra.export.core.ExportInfo;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.util.WellProperty;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
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
			DataFormatter dataFormatter = DataTypePrefs.getDefaultDataFormatter();
			
			// Depending on the filetype, an Excel file or CSV will be created.
			IExportWriter writer;
			if (settings.getFileLocation().toLowerCase().endsWith("csv")) {
				writer = new SubWellDataCSVWriter();
			} else if (settings.getFileLocation().toLowerCase().endsWith("h5")) {
				writer = new SubWellDataH5Writer();
			} else {
				writer = new SubWellDataXLSXWriter();
			}
			
			List<Experiment> experiments = PlateService.streamableList(wells).stream()
					.map(well -> well.getPlate().getExperiment())
					.distinct()
					.collect(Collectors.toList());
			writer.initialize(new BaseExportExperimentsSettings(experiments), dataFormatter);
			writer.addExportInfo(new ExportInfo(new StringValueDescription("Concentration Unit of Well Compounds", ExportInfo.class),
					dataFormatter.getConcentrationUnit(WellProperty.Concentration.getDataDescription()).getAbbr() ));
			
			writer.write(filteredWells, settings);
		}
	}

}
