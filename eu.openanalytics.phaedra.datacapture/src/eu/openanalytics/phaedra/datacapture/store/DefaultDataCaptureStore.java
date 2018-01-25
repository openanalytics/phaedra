package eu.openanalytics.phaedra.datacapture.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.openanalytics.phaedra.base.fs.store.FileStoreFactory;
import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedFeature;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedModel;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedPlate;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedSubWellDataset;
import eu.openanalytics.phaedra.datacapture.parser.model.ParsedWell;
import eu.openanalytics.phaedra.datacapture.store.persist.IDataPersistor;
import eu.openanalytics.phaedra.datacapture.store.persist.ImageDataPersistor;
import eu.openanalytics.phaedra.datacapture.store.persist.PlateDataPersistor;
import eu.openanalytics.phaedra.datacapture.store.persist.SubWellDataPersistor;
import eu.openanalytics.phaedra.datacapture.store.persist.SubWellHDF5DataPersistor;
import eu.openanalytics.phaedra.datacapture.store.persist.WellDataPersistor;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.subwell.SubWellService;

public class DefaultDataCaptureStore implements IDataCaptureStore {

	private IFileStore store;
	private Set<String> tempFolders;
	
	public static final String PLATE_PROPERTY_PREFIX = "plate.properties.";
	public static final String WELL_PROPERTY_PREFIX = "well.properties.";
	public static final String WELL_DATA_PREFIX = "plate.welldata.";
	public static final String WELL_SWDATA_PREFIX = "well.subwelldata.";
	public static final String KEY_IMAGE_PATH = "plate.imagedata.path";
	
	@Override
	public void initialize(PlateReading reading) throws DataCaptureException {
		try {
			tempFolders = new HashSet<>();
			initializeFileStore();
		} catch (IOException e) {
			throw new DataCaptureException("Failed to create a temporary data capture store", e);
		}
	}

	@Override
	public void finish(Plate plate) throws DataCaptureException {
		try {
			store.switchMode();
			for (IDataPersistor persistor: getDataPersistors()) {
				persistor.persist(store, plate);
			}
			cleanupTempFolders();
		} catch (IOException e) {
			throw new DataCaptureException("Failed to save persist captured data", e);
		} finally {
			try { store.close(); } catch (Exception e) {}
		}
	}

	@Override
	public void rollback() {
		cleanupTempFolders();
		try { store.close(); } catch (Exception e) {}
	}

	@Override
	public void saveModel(ParsedModel model) throws DataCaptureException {
		ParsedPlate plate = model.getPlate(0);
		try {
			for (String prop: plate.getPropertyNames()) {
				store.writeValue(PLATE_PROPERTY_PREFIX + prop, plate.getProperty(prop));
			}
			
			ParsedWell[] wells = plate.getWells();
			String[] featureIds = getWellFeatures(wells);
			for (String featureId: featureIds) {
				if (isNumeric(featureId, wells)) {
					float[] numericValues = new float[plate.getRows()*plate.getColumns()];
					Arrays.fill(numericValues, Float.NaN);
					for (ParsedWell well: wells) {
						int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), plate.getColumns());
						ParsedFeature feature = well.getFeature(featureId);
						if (feature != null && feature.getNumericValue() != null) {
							numericValues[wellNr-1] = feature.getNumericValue().floatValue();
						}
					}
					store.writeValue(WELL_DATA_PREFIX + featureId, numericValues);
				} else {
					String[] stringValues = new String[plate.getRows()*plate.getColumns()];
					for (ParsedWell well: wells) {
						int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), plate.getColumns());
						ParsedFeature feature = well.getFeature(featureId);
						String s = feature == null ? null : feature.getStringValue();
						stringValues[wellNr-1] = s;
					}
					store.writeValue(WELL_DATA_PREFIX + featureId, stringValues);
				}
			}
			
			for (ParsedWell well: plate.getWells()) {
				int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), plate.getColumns());

				for (String swFeature: well.getSubWellData().keySet()) {
					String key = WELL_SWDATA_PREFIX + wellNr + "." + swFeature;
					ParsedSubWellDataset ds = well.getSubWellDataset(swFeature);
					if (ds.isNumeric()) {
						store.writeValue(key, ds.getNumericValues());
					} else {
						store.writeValue(key, ds.getStringValues());
					}
				}
				
				for (Entry<String, String> e: well.getKeywords().entrySet()) {
					store.writeValue(WELL_PROPERTY_PREFIX + wellNr + "." + e.getKey(), e.getValue());
				}
			}
		} catch (IOException e) {
			throw new DataCaptureException("Failed to save plate model", e);
		}
	}

	@Override
	public void saveImage(String imagePath) throws DataCaptureException {
		try {
			String outputPath = FileUtils.generateTempFolder(true);
			tempFolders.add(outputPath);
			Files.move(Paths.get(imagePath), Paths.get(outputPath, FileUtils.getName(imagePath)));
			store.writeValue(KEY_IMAGE_PATH, outputPath + "/" + FileUtils.getName(imagePath));
		} catch (IOException e) {
			throw new DataCaptureException("Failed to save image path " + imagePath, e);
		}
	}
	
	@Override
	public void setProperty(String name, Object value) throws DataCaptureException {
		try {
			store.writeValue(PLATE_PROPERTY_PREFIX + name, value);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to save property " + name, e);
		}
	}

	@Override
	public String[] getWellFeatures() throws DataCaptureException {
		try {
			return Arrays.stream(store.listKeys())
					.filter(k -> k.startsWith(WELL_DATA_PREFIX))
					.map(k -> k.substring(WELL_DATA_PREFIX.length()))
					.distinct()
					.sorted()
					.toArray(i -> new String[i]);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to list well features", e);
		}
	}
	
	@Override
	public String[] getSubWellFeatures() throws DataCaptureException {
		try {
			return Arrays.stream(store.listKeys())
					.filter(k -> k.startsWith(WELL_SWDATA_PREFIX))
					.map(k -> {
						String name = k.substring(WELL_SWDATA_PREFIX.length());
						return name.substring(name.indexOf('.') + 1);
					})
					.distinct()
					.sorted()
					.toArray(i -> new String[i]);
		} catch (IOException e) {
			throw new DataCaptureException("Failed to list subwell features", e);
		}
	}
	
	private void initializeFileStore() throws IOException {
		if (SubWellService.getInstance().isHDF5DataSource()) {
			store = new HDF5FileStore();
		} else {
			store = FileStoreFactory.open(null, AccessMode.WRITE, null);			
		}
	}

	private IDataPersistor[] getDataPersistors() {
		IDataPersistor[] persistors = {
				new PlateDataPersistor(),
				new WellDataPersistor(),
				new SubWellDataPersistor(),
				new ImageDataPersistor()
		};
		if (SubWellService.getInstance().isHDF5DataSource()) persistors[2] = new SubWellHDF5DataPersistor();
		return persistors;
	}

	private void cleanupTempFolders() {
		if (tempFolders == null) return;
		for (String file: tempFolders) {
			FileUtils.deleteRecursive(new File(file));
		}
	}
	
	private String[] getWellFeatures(ParsedWell[] wells) {
		Set<String> distinctFeatures = Arrays.stream(wells)
				.flatMap(w -> w.getFeatureIds() == null ? Stream.empty() : Arrays.stream(w.getFeatureIds()))
				.collect(Collectors.toSet());
		return distinctFeatures.toArray(new String[distinctFeatures.size()]);
	}
	
	private boolean isNumeric(String feature, ParsedWell[] wells) {
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
