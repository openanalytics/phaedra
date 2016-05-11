package eu.openanalytics.phaedra.link.data;

import java.util.List;
import java.util.Map;

import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class DataLinkTask {
	
	public boolean createNewPlates;
	public boolean createMissingWellFeatures;
	public boolean createMissingSubWellFeatures;
	
	public Experiment targetExperiment;
	
	public List<PlateReading> selectedReadings;
	public Map<PlateReading, Plate> mappedReadings;
	
	public boolean linkPlateData;
	public boolean linkWellData;
	public boolean linkSubWellData;
	public boolean linkImageData;
}
