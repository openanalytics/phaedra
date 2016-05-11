package eu.openanalytics.phaedra.datacapture.store;

import java.io.InputStream;

import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;

public interface IDataCaptureStore {

	public void initialize(PlateReading reading) throws DataCaptureException;
	public void finish() throws DataCaptureException;
	public void rollback();

	public void saveModel(ParsedModel model) throws DataCaptureException;
	
	public void addImage(String imagePath, boolean allowMove) throws DataCaptureException;
	
	public void addExtraData(String name, InputStream contents) throws DataCaptureException;
	public void addExtraData(String name, InputStream contents, long size) throws DataCaptureException;
	public void addExtraData(String name, byte[] contents) throws DataCaptureException;

	public String[] getStringData(String path) throws DataCaptureException;
	public void addStringData(String path, String[] data) throws DataCaptureException;
	
	public void setProperty(String path, String name, String value) throws DataCaptureException;
	public void setProperty(String path, String name, float value) throws DataCaptureException;
	public void setProperty(String path, String name, float[] value) throws DataCaptureException;
	public void setProperty(String path, String name, int value) throws DataCaptureException;
	public Object getProperty(String path, String name);
	
	public String[] getElements(String path) throws DataCaptureException;
	
	public boolean isDataNumeric(String path) throws DataCaptureException;
	public float[] getNumericData(String path) throws DataCaptureException;
	
	/**
	 * @deprecated Use saveModel(ParsedOutput model) instead.
	 */
	public void addWellData(ParsedModel model) throws DataCaptureException;
	
	/**
	 * @deprecated Use saveModel(ParsedOutput model) instead.
	 */
	public void addSubWellData(int wellNr, ParsedModel model) throws DataCaptureException;

}
