package eu.openanalytics.phaedra.export.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;

public class ExportSettings {

	public String destinationPath;
	public String fileType;
	
	public boolean compoundNameSplit;
	
	public List<Experiment> experiments;
	public List<Feature> features;
	
	public String library;
	public String plateQualifier;
	
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
	
	public enum Includes {
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
		
		public String getLabel() {
			return label;
		}
		
		public boolean isDefaultValue() {
			return defaultValue;
		}
	}
}
