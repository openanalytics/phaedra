package eu.openanalytics.phaedra.export.core;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public class BaseExportExperimentsSettings implements IExportExperimentsSettings {
	
	private String destinationPath;
	private String fileType;
	
	private List<Experiment> experiments;
	
	
	public BaseExportExperimentsSettings(List<Experiment> experiments) {
		this.experiments = experiments;
	}
	
	
	@Override
	public List<Experiment> getExperiments() {
		return experiments;
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
	
}
