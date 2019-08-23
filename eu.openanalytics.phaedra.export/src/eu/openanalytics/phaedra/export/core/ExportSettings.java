package eu.openanalytics.phaedra.export.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * Settings for well data export.
 * 
 * @see Exporter
 */
public class ExportSettings implements IExportExperimentsSettings, IFilterPlatesSettings {


	public enum Includes implements ISettingsOption {
		RawValue("Raw Feature Values", true),
		NormalizedValue("Normalized Feature Values", true),
		Saltform("Saltform", true),
		PlateStatistics("Plate Statistics", true),
		CurveProperties("Curve Properties (Basic)", true),
		CurvePropertiesAll("Curve Properties (All)", false);
		
		private String label;
		private boolean defaultValue;
		
		private Includes(String label, boolean defaultValue) {
			this.label = label;
			this.defaultValue = defaultValue;
		}
		
		@Override
		public String getLabel() {
			return label;
		}
		
		@Override
		public boolean getDefaultValue() {
			return defaultValue;
		}
	}


	public String destinationPath;
	public String fileType;
	
	public boolean compoundNameSplit;
	
	public List<Experiment> experiments;
	public List<Feature> features = new ArrayList<>();
	
	public boolean filterValidation;
	public String validationUser;
	public Date validationDateFrom;
	public Date validationDateTo;
	
	public boolean filterApproval;
	public String approvalUser;
	public Date approvalDateFrom;
	public Date approvalDateTo;
	
	public boolean includeInvalidatedPlates;
	public boolean includeDisapprovedPlates;
	public boolean includeRejectedWells;
	public boolean includeInvalidatedCompounds;
	
	public boolean filterWellResults;
	public Feature wellResultFeature;
	public String wellResultNormalization;
	public String wellResultOperator;
	public String wellResultValue;
	
	public String[] wellTypes;
	
	public boolean filterCompound;
	public String[] compoundTypes;
	public String[] compoundNumbers;
	
	public List<Includes> includes = new ArrayList<>();
	
	
	public ExportSettings(List<Experiment> experiments) {
		this.experiments = experiments;
	}
	
	public ExportSettings() {
	}
	
	
	@Override
	public List<Experiment> getExperiments() {
		return experiments;
	}
	
	public List<Feature> getFeatures() {
		return features;
	}
	
	
	@Override
	public String getDestinationPath() {
		return destinationPath;
	}
	@Override
	public void setDestinationPath(String path) {
		this.destinationPath = path;
	}
	
	@Override
	public String getFileType() {
		return fileType;
	}
	@Override
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	
	@Override
	public boolean getFilterValidation() {
		return filterValidation;
	}
	@Override
	public void setFilterValidation(boolean enable) {
		this.filterValidation = enable;
	}
	
	@Override
	public String getValidationUser() {
		return validationUser;
	}
	@Override
	public void setValidationUser(String user) {
		this.validationUser = user;
	}
	
	@Override
	public Date getValidationDateFrom() {
		return validationDateFrom;
	}
	@Override
	public void setValidationDateFrom(Date data) {
		this.validationDateFrom = data;
	}
	
	@Override
	public Date getValidationDateTo() {
		return validationDateTo;
	}
	@Override
	public void setValidationDateTo(Date date) {
		this.validationDateTo = date;
	}
	
	
	@Override
	public boolean getFilterApproval() {
		return filterApproval;
	}
	@Override
	public void setFilterApproval(boolean enable) {
		this.filterApproval = enable;
	}
	
	@Override
	public String getApprovalUser() {
		return approvalUser;
	}
	@Override
	public void setApprovalUser(String user) {
		this.approvalUser = user;
	}
	
	@Override
	public Date getApprovalDateFrom() {
		return approvalDateFrom;
	}
	@Override
	public void setApprovalDateFrom(Date date) {
		this.approvalDateFrom = date;
	}
	
	@Override
	public Date getApprovalDateTo() {
		return approvalDateTo;
	}
	@Override
	public void setApprovalDateTo(Date date) {
		this.approvalDateTo = date;
	}
	
	
	@Override
	public boolean getIncludeInvalidatedPlates() {
		return this.includeInvalidatedPlates;
	}
	@Override
	public void setIncludeInvalidatedPlates(boolean enable) {
		this.includeInvalidatedPlates = enable;
	}
	
	@Override
	public boolean getIncludeDisapprovedPlates() {
		return includeDisapprovedPlates;
	}
	@Override
	public void setIncludeDisapprovedPlates(boolean enable) {
		this.includeDisapprovedPlates = enable;
	}
	
	
	public List<Includes> getIncludes() {
		return includes;
	}
	
}
