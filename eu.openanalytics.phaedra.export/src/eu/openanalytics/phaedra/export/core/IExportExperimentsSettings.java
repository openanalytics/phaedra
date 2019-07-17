package eu.openanalytics.phaedra.export.core;

import java.util.List;

import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public interface IExportExperimentsSettings {
	
	
	List<Experiment> getExperiments();
	
	String getFileType();
	void setFileType(String fileType);
	
	String getDestinationPath();
	void setDestinationPath(String path);
	
}
