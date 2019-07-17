package eu.openanalytics.phaedra.export.core;

import java.util.Date;

public class FilterPlatesValidator {
	
	public void validate(IFilterPlatesSettings settings) throws ExportException {
		if (settings.getFilterValidation()) {
			Date from = settings.getValidationDateFrom();
			Date to = settings.getValidationDateTo();
			if (from != null && to != null && from.compareTo(to) > 0) {
				throw new ExportException("Invalid validation date range: from > to");
			}
		}
		
		if (settings.getFilterApproval()) {
			Date from = settings.getApprovalDateFrom();
			Date to = settings.getApprovalDateTo();
			if (from != null && to != null && from.compareTo(to) > 0) {
				throw new ExportException("Invalid approval date range: from > to");
			}
		}
	}
	
}
