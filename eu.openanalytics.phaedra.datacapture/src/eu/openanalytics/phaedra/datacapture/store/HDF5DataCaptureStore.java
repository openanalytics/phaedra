package eu.openanalytics.phaedra.datacapture.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedFeature;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedPlate;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedSubWellDataset;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedWell;

public class HDF5DataCaptureStore implements IDataCaptureStore {

	private PlateReading reading;
	
	private String tempFolder;
	private String hdf5Path;
	private HDF5File hdf5File;
	private String imagePath;
	private Lock lock;
	
	private final static String CAPTURE_STORE_PATH = "/data.capture.store";
		
	@Override
	public void initialize(PlateReading reading) throws DataCaptureException {
		this.reading = reading;
		this.tempFolder = FileUtils.generateTempFolder(true);
		this.hdf5Path = tempFolder + "/store.h5";
		this.lock = new ReentrantLock();
		try {
			hdf5File = new HDF5File(hdf5Path, false);
		} catch (Exception e) {
			throw new DataCaptureException("Failed to create new HDF5 file.", e);
		}
	}
	
	@Override
	public void finish() throws DataCaptureException {
		hdf5File.close();
		boolean imageExists = imagePath != null && new File(imagePath).isFile();
		
		String uuid = UUID.randomUUID().toString();
		String baseDestinationPath = CAPTURE_STORE_PATH + "/" + FileUtils.createYearWeekString() + "/" + uuid + ".";
		
		// Upload the file(s).
		try {
			Screening.getEnvironment().getFileServer().putContents(baseDestinationPath + "h5", new File(hdf5Path));
			if (imageExists) {
				String ext = FileUtils.getExtension(imagePath);
				Screening.getEnvironment().getFileServer().putContents(baseDestinationPath + ext, new File(imagePath));
			}
		} catch (IOException e) {
			throw new DataCaptureException("File upload failed at " + baseDestinationPath + "*", e);
		}
		
		FileUtils.deleteRecursive(tempFolder);
		reading.setCapturePath(baseDestinationPath + "h5");
	}

	@Override
	public void rollback() {
		if (hdf5File != null) hdf5File.close();
		if (tempFolder != null) FileUtils.deleteRecursive(tempFolder);
	}
	
	@Override
	public void saveModel(ParsedModel model) throws DataCaptureException {
		if (model == null || model.getPlates() == null || model.getPlates().length == 0) return;
		
		ParsedPlate plate = model.getPlate(0);
		addWellData(plate);
		for (ParsedWell well: plate.getWells()) {
			addSubWellData(well, plate);
		}
		addMetaData(plate);
	}
	
	private void addWellData(ParsedPlate plate) throws DataCaptureException {
		ParsedWell[] wells = plate.getWells();
		if (wells == null || wells.length == 0) return;
		
		String[] featureIds = getAvailableFeatures(wells);
		for (String featureId: featureIds) {
			boolean isNumeric = isNumeric(featureId, wells);
			
			lock.lock();
			try {
				if (isNumeric) {
					float[] numericValues = new float[plate.getRows()*plate.getColumns()];
					for (ParsedWell well: wells) {
						int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), plate.getColumns());
						ParsedFeature feature = well.getFeature(featureId);
						Float v = Float.NaN;
						if (feature != null && feature.getNumericValue() != null) {
							v = feature.getNumericValue();
						}
						numericValues[wellNr-1] = v.floatValue();
					}
					hdf5File.writeWellData(numericValues, featureId);
				} else {
					String[] stringValues = new String[plate.getRows()*plate.getColumns()];
					for (ParsedWell well: wells) {
						int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), plate.getColumns());
						ParsedFeature feature = well.getFeature(featureId);
						String s = feature == null ? null : feature.getStringValue();
						stringValues[wellNr-1] = s;
					}
					hdf5File.writeWellData(stringValues, featureId);
				}
			} catch (IOException e) {
				throw new DataCaptureException("Failed to capture well data", e);
			} finally {
				lock.unlock();
			}
		}
	}

	private void addSubWellData(ParsedWell well, ParsedPlate plate) throws DataCaptureException {
		if (well == null) return;
		
		int row = well.getRow();
		int col = well.getColumn();
		int wellNr = NumberUtils.getWellNr(row, col, plate.getColumns());
		
		Map<String, ParsedSubWellDataset> data = well.getSubWellData();
		
		lock.lock();
		try {
			for (String featureId: data.keySet()) {
				ParsedSubWellDataset dataSet = data.get(featureId);
					String path = HDF5File.getSubWellDataPath(wellNr, featureId);
					int tpCount = dataSet.getTimepoints();
					if (dataSet.isNumeric()) {
						if (tpCount > 1) hdf5File.writeNumericData(path, dataSet.getAllNumericValues());
						else hdf5File.writeNumericData(path, dataSet.getNumericValues());
					} else {
						//TODO Support 2D string data.
						hdf5File.writeStringData1D(path, dataSet.getStringValues());
					}
			}
		} catch (IOException e) {
			throw new DataCaptureException("Failed to capture sub-well data", e);
		} finally {
			lock.unlock();
		}
	}
	
	private void addMetaData(ParsedPlate plate) throws DataCaptureException {
		Map<String, Map<String, String>> metadata = new HashMap<>();
		for (ParsedWell well : plate.getWells()) {
			if (well == null) continue;
			if (well.getKeywords().size() < 1) continue;
			String wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), plate.getColumns()) + "";
			metadata.put(wellNr, well.getKeywords());
		}
		
		lock.lock();
		try {
			hdf5File.writeMetaData(metadata, true);
		} catch (NumberFormatException | IOException e) {
			throw new DataCaptureException("Failed to capture meta-well data", e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void addImage(String newImagePath, boolean allowMove) throws DataCaptureException {
		String ext = FileUtils.getExtension(newImagePath);
		imagePath = tempFolder + "/image." + ext;
		
		boolean moved = false;
		if (allowMove) {
			// If we are allowed to move the file instead of copy it, we save a lot of time.
			moved = new File(newImagePath).renameTo(new File(imagePath));
		}
		if (!moved) {
			try (FileOutputStream out = new FileOutputStream(imagePath)) {
				FileUtils.copy(newImagePath, imagePath);
			} catch (IOException e) {
				throw new DataCaptureException("Failed to copy image: " + newImagePath, e);
			}
		}
	}

	@Override
	public void addExtraData(String name, InputStream contents) throws DataCaptureException {
		lock.lock();
		try {
			hdf5File.writeExtraData(contents, name);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to add data to HDF5 file: " + name, e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void addExtraData(String name, byte[] contents) throws DataCaptureException {
		lock.lock();
		try {
			hdf5File.writeExtraData(contents, name);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to add data to HDF5 file: " + name, e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void addExtraData(String name, InputStream contents, long size) throws DataCaptureException {
		lock.lock();
		try {
			hdf5File.writeExtraData(contents, name, size);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to add data to HDF5 file: " + name, e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void addStringData(String path, String[] data) throws DataCaptureException {
		lock.lock();
		try {
			hdf5File.writeStringData1D(path, data);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to add data to HDF5 file: " + path, e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void setProperty(String path, String name, float value) throws DataCaptureException {
		lock.lock();
		try {
			hdf5File.setAttribute(path, name, value);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to set property in HDF5 file: " + name, e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void setProperty(String path, String name, float[] value) throws DataCaptureException {
		lock.lock();
		try {
			hdf5File.setAttribute(path, name, value);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to set property in HDF5 file: " + name, e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void setProperty(String path, String name, int value) throws DataCaptureException {
		lock.lock();
		try {
			hdf5File.setAttribute(path, name, value);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to set property in HDF5 file: " + name, e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void setProperty(String path, String name, String value) throws DataCaptureException {
		lock.lock();
		try {
			hdf5File.setAttribute(path, name, value);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to set property in HDF5 file: " + name, e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public Object getProperty(String path, String name) {
		lock.lock();
		try {
			return hdf5File.getAttribute(path, name);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public String[] getElements(String path) throws DataCaptureException {
		lock.lock();
		try {
			return hdf5File.getChildren(path);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to get elements at: " + path, e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public boolean isDataNumeric(String path) throws DataCaptureException {
		lock.lock();
		try {
			return hdf5File.isDataNumeric(path);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public float[] getNumericData(String path) throws DataCaptureException {
		lock.lock();
		try {
			return hdf5File.getNumericData1D(path);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to read numeric data at: " + path, e);
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public String[] getStringData(String path) throws DataCaptureException {
		lock.lock();
		try {
			return hdf5File.getStringData1D(path);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to read string data at: " + path, e);
		} finally {
			lock.unlock();
		}
	}
	
	/*
	 * Deprecated methods, see IDataCaptureStore
	 */
	
	@Override
	public void addWellData(ParsedModel model) throws DataCaptureException {
		addWellData(model.getPlate(0));
	}
	
	@Override
	public void addSubWellData(int wellNr, ParsedModel model) throws DataCaptureException {
		ParsedPlate plate = model.getPlate(0);
		int cols = plate.getColumns();
		if (cols == 0) cols = 12;
		int[] pos = NumberUtils.getWellPosition(wellNr, cols);
		// Assuming here that the subwelldata parser stored the data in well A1 (deprecated method, use saveModel() instead).
		ParsedWell well = plate.getWell(1, 1);
		well.setRow(pos[0]);
		well.setColumn(pos[1]);
		addSubWellData(well, plate);
	}
	
	/*
	 * Non-public
	 */
	
	private static String[] getAvailableFeatures(ParsedWell[] wells) {
		List<String> featureIds = new ArrayList<String>();
		for (ParsedWell well: wells) {
			if (well.getFeatureIds() == null) continue;
			
			for (String id: well.getFeatureIds()) {
				CollectionUtils.addUnique(featureIds, id);
			}
		}
		return featureIds.toArray(new String[featureIds.size()]);
	}
	
	private static boolean isNumeric(String feature, ParsedWell[] wells) {
		int wellCount = wells.length;
		int numericCount = 0;
		int stringCount = 0;
		int nullCount = 0;
		
		for (ParsedWell well: wells) {
			ParsedFeature f = well.getFeature(feature);
			if (f == null) {
				nullCount++;
				continue;
			}
			Float numVal = f.getNumericValue();
			if (numVal == null) {
				String stringVal = f.getStringValue();
				if (stringVal != null) stringCount++;
				else nullCount++;
			} else {
				numericCount++;
			}
		}
		
		if (wellCount == nullCount) return true;
		if (wellCount == numericCount) return true;
		if (numericCount > 0 && stringCount == 0) return true;
		
		return false;
	}
}