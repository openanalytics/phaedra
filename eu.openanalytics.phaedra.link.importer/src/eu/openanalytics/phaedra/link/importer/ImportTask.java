package eu.openanalytics.phaedra.link.importer;

import java.util.HashMap;
import java.util.Map;

import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class ImportTask {

	// The file or folder to import.
	public String sourcePath;

	// The import destination.
	public Experiment targetExperiment;
	
	// The user who performs the import.
	public String userName;
	
	// An optional import remark, applied to each plate.
	public String remark;
	
	// True to create new plates, false to map to existing plates.
	public boolean createNewPlates;

	// If createNewPlates is false, use this reading -> plate mapping.
	public Map<PlateReading, Plate> plateMapping;
	
	// The import steps to execute or skip (if the data capture config supports skipping modules).
	public boolean importPlateData;
	public boolean importWellData;
	public boolean importImageData;
	public boolean importSubWellData;
	
	// If null, will be set in ImportJob.createCaptureTask() using protocol(-class)/data capture mapping
	// otherwise the specified id data capture config will be used in the import
	private String captureConfigId;
	
	private Map<String, Object> parameters;
	
	public ImportTask() {
		createNewPlates 	= true;
		importPlateData 	= true;
		importWellData 		= true;
		importImageData 	= true;
		importSubWellData 	= true;
		captureConfigId 	= null; //default: use protocol(-class)/data capture mapping to load data capture config
		
		parameters = new HashMap<>();
	}
	
	public void setCaptureConfigId(String captureConfigId) {
		this.captureConfigId = captureConfigId;
	}
	
	public String getCaptureConfigId() {
		if (captureConfigId == null) {
			if (targetExperiment == null) {
				throw new RuntimeException("Cannot determine a capture configuration id without the target experiment being specified.");
			}
			return ImportUtils.getCaptureConfigId(targetExperiment.getProtocol());
		} else {
			return captureConfigId;
		}
	}
	
	public Map<String, Object> getParameters() {
		return parameters;
	}
}
