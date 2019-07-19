package eu.openanalytics.phaedra.export.core;

import java.util.Date;

public interface IFilterPlatesSettings extends IExportExperimentsSettings {
	
	
	String getLibrary();
	void setLibrary(String library);
	
	String getPlateQualifier();
	void setPlateQualifier(String qualifier);
	
	
	boolean getFilterValidation();
	void setFilterValidation(boolean enable);
	
	String getValidationUser();
	void setValidationUser(String user);
	
	Date getValidationDateFrom();
	void setValidationDateFrom(Date date);
	Date getValidationDateTo();
	void setValidationDateTo(Date date);
	
	
	boolean getFilterApproval();
	void setFilterApproval(boolean enable);
	
	String getApprovalUser();
	void setApprovalUser(String user);
	
	Date getApprovalDateFrom();
	void setApprovalDateFrom(Date date);
	Date getApprovalDateTo();
	void setApprovalDateTo(Date date);
	
	
	void setIncludeInvalidatedPlates(boolean enable);
	boolean getIncludeInvalidatedPlates();
	
	void setIncludeDisapprovedPlates(boolean enable);
	boolean getIncludeDisapprovedPlates();
	
}
