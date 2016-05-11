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
	
	public boolean filterCurveResults;
	public Feature curveFeature;
	public String curveOperator;
	public String curveValue;
	
	public boolean filterCompound;
	public String[] compoundTypes;
	public String[] compoundNumbers;
	
	public List<Includes> includes = new ArrayList<>();
	
	public enum Includes {
		RawValue("Raw Feature Values"),
		NormalizedValue("Normalized Feature Values"),
		Saltform("Saltform"),
		PlateStatistics("Plate Statistics"),
		DrcMethod("DRC Method"),
		DrcModel("DRC Model"),
		DrcType("DRC Type"),
		Pic50Plac("pIC50/pLAC"),
		Hill("Hill"),
		R2("R2"),
		Threshold("Threshold"),
		Emax("EMAX"),
		LbUb("Lb and Ub"),
		LbUbStdErr("Lb and Ub Standard Errors");
		
		private String label;
		
		private Includes(String label) {
			this.label = label;
		}
		
		public String getLabel() {
			return label;
		}
	}
}
