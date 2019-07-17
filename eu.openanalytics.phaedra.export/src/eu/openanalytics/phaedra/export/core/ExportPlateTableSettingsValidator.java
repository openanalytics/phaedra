package eu.openanalytics.phaedra.export.core;

import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.WriterFactory;

public class ExportPlateTableSettingsValidator {
	
	private final FilterPlatesValidator filterPlatesValidator= new FilterPlatesValidator();
	
	
	public void validate(ExportPlateTableSettings settings) throws ExportException {
		if (settings.getExperiments() == null || settings.getExperiments().isEmpty()) {
			throw new ExportException("No experiment(s) provided");
		}
		
		filterPlatesValidator.validate(settings);
		
		IExportWriter writer = WriterFactory.createWriter(settings.getFileType());
		if (writer == null) {
			throw new ExportException("No appropriate writer found for file type " + settings.getFileType());
		}
	}
	
}
