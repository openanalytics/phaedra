package eu.openanalytics.phaedra.export.core.subwell;

import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class ExportSettings {
	
	private List<SubWellFeature> selectedFeatures;
	private int exportMode;
	private Double subsetProc;
	private boolean includeRejected;
	private int includedWellTypes;
	private String fileLocation;
	
	public ExportSettings() {
		this.selectedFeatures = new ArrayList<>();
		this.exportMode = IExportWriter.MODE_ONE_PAGE;
		this.subsetProc = null;
		this.includeRejected = false;
		this.includedWellTypes = IExportWriter.WELLTYPE_ALL;
	}
	
	public List<SubWellFeature> getSelectedFeatures() {
		return selectedFeatures;
	}
	
	public void setSelectedFeatures(List<SubWellFeature> selectedFeatures) {
		this.selectedFeatures = selectedFeatures;
	}
	
	public int getExportMode() {
		return exportMode;
	}
	
	public void setExportMode(int exportMode) {
		this.exportMode = exportMode;
	}
	
	public Double getSubsetProc() {
		return subsetProc;
	}
	
	public void setSubsetProc(Double subsetProc) {
		this.subsetProc = subsetProc;
	}
	
	public boolean isIncludeRejected() {
		return includeRejected;
	}
	
	public void setIncludeRejected(boolean includeRejected) {
		this.includeRejected = includeRejected;
	}
	
	public int getIncludedWellTypes() {
		return includedWellTypes;
	}
	
	public void setIncludedWellTypes(int includedWellTypes) {
		this.includedWellTypes = includedWellTypes;
	}
	
	public String getFileLocation() {
		return fileLocation;
	}
	
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}
	
}