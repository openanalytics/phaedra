package eu.openanalytics.phaedra.export.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

/**
 * Settings for plate list export.
 * 
 * @see PlateTableExporter
 */
public class ExportPlateTableSettings extends BaseExportExperimentsSettings implements IExportExperimentsSettings, IFilterPlatesSettings {
	
	public enum Includes implements ISettingsOption {
		PlateSummary("Plate Summary (#DRC, #SDP)", true),
		ApproveAndValidationDetail("Approve and Validation Detail", false),
		FeatureStatistics("Feature Statistics (Z-Prime, S/B, S/N)", true),
		FeatureControlStatistics("Feature LC/HC Statistics (Mean, %CV)", false);
		
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
	
	
	private List<Feature> features = new ArrayList<>();
	
	private String library;
	private String plateQualifier;
	
	private boolean filterValidation;
	private String validationUser;
	private Date validationDateFrom;
	private Date validationDateTo;
	
	private boolean filterApproval;
	private String approvalUser;
	private Date approvalDateFrom;
	private Date approvalDateTo;
	
	private boolean includeInvalidatedPlates;
	private boolean includeDisapprovedPlates;
	
	private List<Includes> includes = new ArrayList<>();
	
	
	public ExportPlateTableSettings(List<Experiment> experiments) {
		super(experiments);
	}
	
	
	public List<Feature> getFeatures() {
		return this.features;
	}
	
	
	@Override
	public String getLibrary() {
		return library;
	}
	@Override
	public void setLibrary(String library) {
		this.library = library;
	}
	
	@Override
	public String getPlateQualifier() {
		return plateQualifier;
	}
	@Override
	public void setPlateQualifier(String plateQualifier) {
		this.plateQualifier = plateQualifier;
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
