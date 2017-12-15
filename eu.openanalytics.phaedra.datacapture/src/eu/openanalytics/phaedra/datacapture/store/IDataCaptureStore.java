package eu.openanalytics.phaedra.datacapture.store;

import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public interface IDataCaptureStore {

	public void initialize(PlateReading reading) throws DataCaptureException;
	public void finish(Plate plate) throws DataCaptureException;
	public void rollback();

	public void saveModel(ParsedModel model) throws DataCaptureException;
	public void saveImage(String imagePath) throws DataCaptureException;
	public void setProperty(String name, Object value) throws DataCaptureException;
	
	public String[] getWellFeatures() throws DataCaptureException;
	public String[] getSubWellFeatures() throws DataCaptureException;
}
