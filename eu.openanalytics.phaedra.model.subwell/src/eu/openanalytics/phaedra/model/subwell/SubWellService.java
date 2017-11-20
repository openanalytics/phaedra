package eu.openanalytics.phaedra.model.subwell;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.environment.prefs.Prefs;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.hdf5.parallel.HDF5MultiProcessReader;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.threading.ThreadPool;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.cache.SubWellDataCache;
import eu.openanalytics.phaedra.model.subwell.util.SubWellModificationTransaction;
import eu.openanalytics.phaedra.validation.ValidationUtils;

/**
 * API for interaction with subwell data. This includes retrieving data and writing or updating data.
 * Subwell data can either be numeric (32bit floating point) or text (String of any length).
 * <p>
 * For performance reasons, subwell data is always returned in arrays. There is one array per well
 * per feature, and the length of the array is equal to the number of subwell items in that well.
 * The array is sorted, so that the id of the subwell item is equal to its index in the array.
 * </p>
 * <p>
 * All data that is retrieved once, is cached for future lookups.
 * See {@link CacheService} for more information about caching.
 * </p>
 */
public class SubWellService  {

	private static SubWellService instance = new SubWellService();

	private SubWellDataCache cache;

	private boolean useParallelReading;
	private HDF5MultiProcessReader parallelReader;
	private ThreadPool parallelReaderTP;

	private SubWellService() {
		// Hidden constructor.
		cache = new SubWellDataCache();
		initPrefListener();
		initParalellReading();
	}

	public static SubWellService getInstance() {
		return instance;
	}

	/**
	 * Get a sample subwell feature that has at least one point of subwell data.
	 * This can be used to determine the count of subwell items in a well, as all
	 * features should have the same amount of data for any given well.
	 * 
	 * @param well The well to retrieve a sample feature for.
	 * @return A sample feature, or null if no feature has data for this well.
	 */
	public SubWellFeature getSampleFeature(Well well) {
		HDF5File hdf5File = null;
		try {
			hdf5File = getDataFile(well.getPlate());
			if (hdf5File == null) return null;
			int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
			for (SubWellFeature feature: PlateUtils.getSubWellFeatures(well.getPlate())) {
				String featureId = feature.getName();
				boolean dataExists = hdf5File.existsSubWellData(featureId, wellNr);
				if (dataExists) return feature;
			}
		} finally {
			if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
		}

		return null;
	}

	/**
	 * Get the data of a feature for a well, be it text or numeric.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @return The data, either a String array, a float array, or null.
	 */
	public Object getAnyData(Well well, SubWellFeature feature) {
		if (well == null || feature == null) return null;

		if (cache.isCached(well, feature)) {
			if (cache.isNumeric(well, feature)) return cache.getNumericData(well, feature);
			else return cache.getStringData(well, feature);
		}

		synchronized (well.getPlate()) {
			// Check cache again: maybe another thread filled the cache while we were waiting for synchronization.
			if (cache.isCached(well, feature)) {
				if (cache.isNumeric(well, feature)) return cache.getNumericData(well, feature);
				else return cache.getStringData(well, feature);
			}

			// Not in cache: read from file and add to cache.
			HDF5File hdf5File = null;
			try {
				hdf5File = getDataFile(well.getPlate());
				if (hdf5File == null) return null;
				int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
				String featureId = feature.getName();
				boolean dataExists = hdf5File.existsSubWellData(featureId, wellNr);
				if (!dataExists) {
					cache.putData(well, feature, (float[])null);
					return null;
				}

				Object value = null;
				if (hdf5File.isSubWellDataNumeric(featureId, wellNr)) {
					float[] numericValues = hdf5File.getNumericSubWellData(featureId, wellNr);
					cache.putData(well, feature, numericValues);
					value = numericValues;
				} else {
					String[] stringValues = hdf5File.getStringSubWellData(featureId, wellNr);
					cache.putData(well, feature, stringValues);
					value = stringValues;
				}

				return value;
			} catch (IOException e) {
				throw new RuntimeException("Failed to load subwell data", e);
			} finally {
				if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
			}
		}
	}

	/**
	 * Get the numeric data of a feature for a well.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The name of the subwell feature to retrieve data for.
	 * @return The numeric data, or null if the well has no data for this feature.
	 */
	public Object getNumericData(Well well, String feature) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		SubWellFeature f = ProtocolUtils.getSubWellFeatureByName(feature, pClass);
		return getNumericData(well, f);
	}

	/**
	 * Get the numeric data of a feature for a well.
	 * Eager loading will be attempted. See {@link SubWellService#getNumericData(Well, SubWellFeature, boolean)}.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @return The numeric data, or null if the well has no data for this feature.
	 */
	public float[] getNumericData(Well well, SubWellFeature feature) {
		return getNumericData(well, feature, true);
	}

	/**
	 * Get the numeric data of a feature for a well.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @param eager True to perform eager loading: pre-load all data for this well.
	 * @return The numeric data, or null if the well has no data for this feature.
	 */
	public float[] getNumericData(Well well, SubWellFeature feature, boolean eager) {
		if (well == null || feature == null) return null;

		if (cache.isCached(well, feature)) {
			if (cache.isNumeric(well, feature)) return cache.getNumericData(well, feature);
			else return null;
		}

		synchronized (well.getPlate()) {
			// Check cache again: maybe another thread filled the cache while we were waiting for synchronization.
			if (cache.isCached(well, feature)) {
				if (cache.isNumeric(well, feature)) return cache.getNumericData(well, feature);
				else return null;
			}

			// Not in cache: read from file and add to cache.
			HDF5File hdf5File = null;
			try {
				hdf5File = getDataFile(well.getPlate());
				if (hdf5File == null) return new float[0];

				int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());

				if (eager) {
					// Eager loading: load all features for this well.
					List<SubWellFeature> allFeatures = PlateUtils.getProtocolClass(well).getSubWellFeatures();
					float[] dataToReturn = null;
					for (SubWellFeature f: allFeatures) {
						boolean cached = cache.isCached(well, f);
						if (f.isNumeric()) {
							float[] data = null;
							if (cached) {
								data = cache.getNumericData(well, f);
							} else {
								data = loadNumericData(hdf5File, f, wellNr, 0);
								cache.putData(well, f, data);
							}
							if (f.equals(feature)) dataToReturn = data;
						} else if (!cached) {
							String[] data = loadStringData(hdf5File, f, wellNr, 0);
							cache.putData(well, f, data);
						}
					}
					return dataToReturn;
				} else {
					// Lazy loading: load only this feature for this well.
					float[] data = loadNumericData(hdf5File, feature, wellNr, 0);
					cache.putData(well, feature, data);
					return data;
				}

			} catch (IOException e) {
				throw new RuntimeException("Failed to load subwell data", e);
			} finally {
				if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
			}
		}
	}

	/**
	 * Load a set of 2-dimensional (single-cell, timepoint-based) subwell data.
	 * 
	 * @deprecated Phaedra does not fully support 2D subwell data.
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @return The 2D numeric data, or null if the well has no 2D data for this feature.
	 */
	public float[][] getNumericData2D(Well well, SubWellFeature feature) {
		if (well == null || feature == null) return null;

		HDF5File hdf5File = null;
		try {
			hdf5File = getDataFile(well.getPlate());
			if (hdf5File == null) return new float[0][];

			int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
			String featureId = feature.getName();
			boolean dataExists = hdf5File.existsSubWellData(featureId, wellNr);
			if (!dataExists){
				cache.putData(well, feature, (float[])null);
				return null;
			}
			if (!hdf5File.isSubWellDataNumeric(featureId, wellNr)) return null;
			String path = HDF5File.getSubWellDataPath(wellNr, featureId);

			float[][] data = hdf5File.getNumericData2D(path);
			return data;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load subwell data", e);
		} finally {
			if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Get the String data of a feature for a well.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The name of the subwell feature to retrieve data for.
	 * @return The String data, or null if the well has no data for this feature.
	 */
	public Object getStringData(Well well, String feature) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		SubWellFeature f = ProtocolUtils.getSubWellFeatureByName(feature, pClass);
		return getStringData(well, f);
	}

	/**
	 * Get the String data of a feature for a well.
	 * 
	 * @param well The well to retrieve data for.
	 * @param feature The subwell feature to retrieve data for.
	 * @return The String data, or null if the well has no data for this feature.
	 */
	public String[] getStringData(Well well, SubWellFeature feature) {
		if (well == null || feature == null) return null;

		if (cache.isCached(well, feature)) {
			if (!cache.isNumeric(well, feature)) return cache.getStringData(well, feature);
			else return null;
		}

		synchronized (well.getPlate()) {
			// Check cache again: maybe another thread filled the cache while we were waiting for synchronization.
			if (cache.isCached(well, feature)) {
				if (!cache.isNumeric(well, feature)) return cache.getStringData(well, feature);
				else return null;
			}

			// Not in cache: read from file and add to cache.
			HDF5File hdf5File = null;
			try {
				hdf5File = getDataFile(well.getPlate());
				if (hdf5File==null) return new String[0];

				int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
				String featureId = feature.getName();
				boolean dataExists = hdf5File.existsSubWellData(featureId, wellNr);
				if (!dataExists){
					cache.putData(well, feature, (String[])null);
					return null;
				}
				if (hdf5File.isSubWellDataNumeric(featureId, wellNr)) return null;
				String[] stringValues = hdf5File.getStringSubWellData(featureId, wellNr);

				cache.putData(well, feature, stringValues);
				return stringValues;
			} catch (IOException e) {
				throw new RuntimeException("Failed to load subwell data", e);
			} finally {
				if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
			}
		}
	}

	/**
	 * Get the number of subwell items for a given well.
	 * 
	 * @param well The well whose number of subwell items should be retrieved.
	 * @return The number of subwell items in the well, possibly 0.
	 */
	public int getNumberOfCells(Well well) {
		if (well == null || !well.getPlate().isSubWellDataAvailable()) return 0;

		ProtocolClass pClass = PlateUtils.getProtocolClass(well);

		HDF5File hdf5File = null;
		try {
			for (SubWellFeature feature : pClass.getSubWellFeatures()) {
				// If the data was already cached, use it
				if (cache.isCached(well, feature)) {
					if (cache.isNumeric(well, feature)) {
						float[] data = cache.getNumericData(well, feature);
						if (data != null) return data.length;
					} else {
						String[] data = cache.getStringData(well, feature);
						if (data != null) return data.length;
					}
				} else {
					if (hdf5File == null) {
						hdf5File = getDataFile(well.getPlate());
						if (hdf5File == null) return 0;
					}

					int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
					String featureId = feature.getName();
					boolean dataExists = hdf5File.existsSubWellData(featureId, wellNr);
					if (dataExists) {
						String path = HDF5File.getSubWellDataPath(wellNr, featureId);
						long[] dims = hdf5File.getDataDimensions(path);
						if (dims.length > 0) {
							return (int) dims[0];
						}
					}
				}
			}
			return 0;
		} finally {
			if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Preload the data for a set of wells and a set of features.
	 * The data will be cached so that future lookups will be very fast.
	 * This may offer better performance than loading the data one well or one feature at a time.
	 * 
	 * @param wells The wells to preload data for.
	 * @param features The features to preload data for.
	 * @param monitor A progress monitor that will be updated during the load (optional)
	 */
	public void preloadData(List<Well> wells, List<SubWellFeature> features, IProgressMonitor monitor) {
		// If the entire plate is already cached, abort the loading process.
		boolean allCached = true;
		for (SubWellFeature f: features) {
			for (Well well: wells) {
				if (!cache.isCached(well, f)) {
					allCached = false;
					break;
				}
			}
		}
		if (allCached) return;

		IProgressMonitor monitorToUse = (monitor == null) ? new NullProgressMonitor() : monitor;
		monitorToUse.beginTask("Loading subwell data for " + wells.size() + " wells", wells.size() + 5);

		// Determine the needed HDF5 files (downloading them if desired).
		Map<Plate, String> hdf5Files = new HashMap<>();
		for (Well well: wells) {
			if (hdf5Files.containsKey(well.getPlate())) continue;
			String hdf5Path = Screening.getEnvironment().getFileServer().getAsFile(getDataPath(well.getPlate())).getAbsolutePath();
			if (new File(hdf5Path).exists()) hdf5Files.put(well.getPlate(), hdf5Path);
		}
		monitorToUse.worked(5);

		if (useParallelReading) {
			List<String> featureNames = features.stream().map(f -> f.getName()).collect(Collectors.toList());
			for (Well well: wells) {
				int wellNr = PlateUtils.getWellNr(well);
				String filePathToUse = hdf5Files.get(well.getPlate());
				if (filePathToUse == null) continue;
				parallelReaderTP.schedule(() -> {
					synchronized (monitorToUse) {
						if (monitorToUse.isCanceled()) return;
					}
					Map<String, float[]> data = parallelReader.read(filePathToUse, featureNames, wellNr);
					for (String f: data.keySet()) {
						SubWellFeature feature = ProtocolUtils.getSubWellFeatureByName(f, features.get(0).getProtocolClass());
						// Feature null may occur if the HDF5 file contains features which are no longer present in the protocol class.
						if (feature != null) cache.putData(well, feature, data.get(f));
					}
					synchronized (monitorToUse) { monitorToUse.worked(1); }
				});
			}
			while (!parallelReaderTP.isIdle()) {
				// Check if the monitor is canceled because the thread pool could be doing other tasks.
				if (monitorToUse.isCanceled()) break;
				try { Thread.sleep(100); } catch (InterruptedException e) {}
			}
		} else {
			for (Well well: wells) {
				if (monitorToUse.isCanceled()) return;
				for (SubWellFeature f: features) getNumericData(well, f, false);
				monitorToUse.worked(1);
			}
		}

		monitorToUse.done();
	}

	/**
	 * Update the subwell data for a set of wells in a single transaction.
	 * 
	 * @param dataMap The map of data to update, containing an array of data (String or float) per well.
	 * @param feature The subwell feature that the data belongs to.
	 * @throws IOException If the update transaction fails for any reason.
	 */
	public void updateData(Map<Well, Object> dataMap, SubWellFeature feature) throws IOException {
		if (feature == null || dataMap.isEmpty()) return;
		Map<SubWellFeature, Map<Well, Object>> data = new HashMap<>();
		data.put(feature, dataMap);
		updateData(data);
	}

	/**
	 * Update the subwell data for a set of wells and features in a single transaction.
	 * 
	 * @param data The map of data to update, containing a sub-map per feature. The sub-map contains
	 * an array of data (String or float) per well.
	 * @throws IOException If the update transaction fails for any reason.
	 */
	public void updateData(Map<SubWellFeature, Map<Well, Object>> data) throws IOException {
		if (data == null || data.isEmpty()) return;
		Well sampleWell = data.values().iterator().next().keySet().iterator().next();
		Plate plate = sampleWell.getPlate();

		SubWellModificationTransaction transaction = createTransaction(plate);
		try {
			transaction.begin();
			updateData(data, transaction);
			transaction.commit();
		} catch (IOException e) {
			transaction.rollback();
			throw e;
		}
	}

	/**
	 * Remove all subwell data for a given plate and feature from the cache.
	 * 
	 * @param plate The plate whose data will be cleared from the cache.
	 * @param feature The feature whose data will be cleared from the cache.
	 */
	public void removeFromCache(Plate plate, SubWellFeature feature) {
		for (Well well: plate.getWells()) {
			removeFromCache(well, feature);
		}
	}

	/**
	 * Remove all subwell data for a given well and feature from the cache.
	 * 
	 * @param well The well whose data will be cleared from the cache.
	 * @param feature The feature whose data will be cleared from the cache.
	 */
	public void removeFromCache(Well well, SubWellFeature feature) {
		cache.removeData(well, feature);
	}

	private String getDataPath(Plate plate) {
		return PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
	}

	private HDF5File getDataFile(Plate plate) {
		String hdf5Path = getDataPath(plate);
		try {
			if (!Screening.getEnvironment().getFileServer().exists(hdf5Path)) return null;
		} catch (IOException e) { return null; }
		return HDF5File.openForRead(hdf5Path);
	}

	private float[] loadNumericData(HDF5File hdf5File, SubWellFeature feature, int wellNr, int timepoint) throws IOException {
		String featureId = feature.getName();
		boolean dataExists = hdf5File.existsSubWellData(featureId, wellNr);
		if (!dataExists || !hdf5File.isSubWellDataNumeric(featureId, wellNr)) {
			return null;
		}

		String path = HDF5File.getSubWellDataPath(wellNr, featureId);
		long[] dimensions = hdf5File.getDataDimensions(path);
		float[] data = null;
		if (dimensions.length == 1) {
			data = hdf5File.getNumericData1D(path);
		} else {
			float[][] timepointData = hdf5File.getNumericData2D(path);
			// Timepoint data: assume 2D dataset.
			if (timepointData.length == 0 || timepoint >= timepointData[0].length) return null;
			data = new float[timepointData.length];
			for (int i=0; i<data.length; i++) data[i] = timepointData[i][timepoint];
		}
		return data;
	}

	private String[] loadStringData(HDF5File hdf5File, SubWellFeature feature, int wellNr, int timepoint) throws IOException {
		String featureId = feature.getName();
		boolean dataExists = hdf5File.existsSubWellData(featureId, wellNr);
		if (!dataExists || hdf5File.isSubWellDataNumeric(featureId, wellNr)) {
			return null;
		}

		String path = HDF5File.getSubWellDataPath(wellNr, featureId);
		long[] dimensions = hdf5File.getDataDimensions(path);
		String[] data = null;
		if (dimensions.length == 1) {
			data = hdf5File.getStringData1D(path);
		}
		return data;
	}

	private SubWellModificationTransaction createTransaction(Plate plate) {
		return new SubWellModificationTransaction(plate);
	}
	
	private void updateData(Map<SubWellFeature, Map<Well, Object>> data, SubWellModificationTransaction transaction) throws IOException {
		if (data == null || data.isEmpty() || transaction == null) return;

		// Plate status check.
		Set<Plate> plates = new HashSet<>();
		for (Map<Well, Object> featureData: data.values()) {
			for (Well well: featureData.keySet()) plates.add(well.getPlate());
		}
		for (Plate plate: plates) ValidationUtils.checkCanModifyPlate(plate);

		// Important: data is REPLACED, so the data objects have to be fully loaded.
		for (SubWellFeature feature: data.keySet()) {
			Map<Well, Object> featureData = data.get(feature);
			for (Well well : featureData.keySet()) {
				Object wellData = featureData.get(well);
				int dataCount = (wellData instanceof float[]) ? ((float[])wellData).length : ((String[])wellData).length;
				if (dataCount == 0) continue;

				int wellNumber = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());

				if (wellData instanceof float[]) {
					float[] items = (float[])wellData;
					transaction.getWorkingCopy().writeSubWellData(items, wellNumber, feature.getName());
				} else {
					String[] items = (String[])wellData;
					transaction.getWorkingCopy().writeSubWellData(items, wellNumber, feature.getName());
				}
				cache.removeData(well, feature);
			}
		}
	}
	
	private void initPrefListener() {
		PrefUtils.getPrefStore().addPropertyChangeListener((event) -> {
			if (event.getProperty().equals(Prefs.USE_PARALLEL_SUBWELL_LOADING)
					|| event.getProperty().equals(Prefs.THREAD_POOL_SIZE)
					|| event.getProperty().equals(Prefs.USE_ALL_LOG_CORES)
					|| event.getProperty().equals(Prefs.USE_ALL_PHYS_CORES)) {
				initParalellReading();
			}
		});
	}

	private void initParalellReading() {
		useParallelReading = PrefUtils.getPrefStore().getBoolean(Prefs.USE_PARALLEL_SUBWELL_LOADING);
		int processCount = PrefUtils.getNumberOfThreads();
		int oldProcessCount = (parallelReader == null) ? 0 : parallelReader.getSlaveCount();

		if (!useParallelReading || processCount != oldProcessCount) {
			if (parallelReader != null) parallelReader.shutdown();
			if (parallelReaderTP != null) parallelReaderTP.stop(false);
			parallelReader = null;
			parallelReaderTP = null;
		}

		if (useParallelReading && parallelReader == null) {
			parallelReader = new HDF5MultiProcessReader(processCount);
			parallelReader.startup();
			parallelReaderTP = new ThreadPool(processCount);
		}
	}
}