package eu.openanalytics.phaedra.export.core;

import java.util.Date;

import eu.openanalytics.phaedra.export.core.writer.IExportWriter;
import eu.openanalytics.phaedra.export.core.writer.WriterFactory;

public class SettingsValidator {

	public void validate(ExportSettings settings) throws ExportException {
		
		if (settings.features == null || settings.features.isEmpty()) {
			throw new ExportException("No feature(s) selected");
		}
		
		if (settings.experiments == null || settings.experiments.isEmpty()) {
			throw new ExportException("No experiment(s) provided");	
		}
				
		if (settings.filterValidation) {
			Date from = settings.validationDateFrom;
			Date to = settings.validationDateTo;
			if (from != null && to != null && from.compareTo(to) > 0) {
				throw new ExportException("Invalid validation date range: from > to");
			}
		}
		
		if (settings.filterApproval) {
			Date from = settings.approvalDateFrom;
			Date to = settings.approvalDateTo;
			if (from.compareTo(to) > 0) {
				throw new ExportException("Invalid approval date range: from > to");
			}
		}
		
		IExportWriter writer = WriterFactory.createWriter(settings.fileType);
		if (writer == null) {
			throw new ExportException("No appropriate writer found for file type " + settings.fileType);
		}
	}
}
