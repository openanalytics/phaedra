package eu.openanalytics.phaedra.model.subwell.data;

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

public class HDF5Datasource implements ISubWellDataSource {

	private boolean useParallelReading;
	private HDF5MultiProcessReader parallelReader;
	private ThreadPool parallelReaderTP;

	public HDF5Datasource() {
		initPrefListener();
		initParalellReading();
	}
	
	@Override
	public void close() {
		parallelReaderTP.stop(false);
	}
	
	@Override
	public int getNrCells(Well well) {
		if (well == null || !well.getPlate().isSubWellDataAvailable()) return 0;

		ProtocolClass pClass = PlateUtils.getProtocolClass(well);

		HDF5File hdf5File = null;
		try {
			for (SubWellFeature feature : pClass.getSubWellFeatures()) {
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
			return 0;
		} finally {
			if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
		}
	}

	@Override
	public float[] getNumericData(Well well, SubWellFeature feature) {
		if (well == null || feature == null) return null;

		synchronized (well.getPlate()) {
			HDF5File hdf5File = null;
			try {
				hdf5File = getDataFile(well.getPlate());
				if (hdf5File == null) return new float[0];

				int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
				return loadNumericData(hdf5File, feature, wellNr);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load subwell data", e);
			} finally {
				if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
			}
		}
	}
	
	@Override
	public String[] getStringData(Well well, SubWellFeature feature) {
		if (well == null || feature == null) return null;

		synchronized (well.getPlate()) {
			HDF5File hdf5File = null;
			try {
				hdf5File = getDataFile(well.getPlate());
				if (hdf5File==null) return new String[0];

				int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), well.getPlate().getColumns());
				return loadStringData(hdf5File, feature, wellNr);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load subwell data", e);
			} finally {
				if (hdf5File != null) try { hdf5File.close(); } catch (Exception e) {}
			}
		}
	}

	@Override
	public void updateData(Map<SubWellFeature, Map<Well, Object>> data) {
		if (data == null || data.isEmpty()) return;
		
		List<Plate> plates = data.values().stream().flatMap(m -> m.keySet().stream()).map(Well::getPlate).distinct().collect(Collectors.toList());
		
		for (Plate plate: plates) {
			Map<SubWellFeature, Map<Well, Object>> dataPerPlate = new HashMap<>();
			
			for (SubWellFeature feature: data.keySet()) {
				Map<Well, Object> dataPerWell = data.get(feature).entrySet().stream()
						.filter(e -> e.getKey().getPlate().equals(plate))
						.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
				if (dataPerWell.isEmpty()) continue;
				dataPerPlate.put(feature, dataPerWell);
			}
			
			if (dataPerPlate.isEmpty()) continue;
			
			SubWellModificationTransaction transaction = new SubWellModificationTransaction(plate);
			try {
				transaction.begin();
				updateData(dataPerPlate, transaction);
				transaction.commit();
			} catch (IOException e) {
				transaction.rollback();
				throw new RuntimeException("Failed to update subwell data", e);
			}
		}
	}
	
	@Override
	public void preloadData(List<Well> wells, List<SubWellFeature> features, SubWellDataCache cache, IProgressMonitor monitor) {
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
				for (SubWellFeature f: features) getNumericData(well, f);
				monitorToUse.worked(1);
			}
		}

		monitorToUse.done();
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

	private float[] loadNumericData(HDF5File hdf5File, SubWellFeature feature, int wellNr) throws IOException {
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
			if (timepointData.length == 0) return null;
			data = new float[timepointData.length];
			for (int i=0; i<data.length; i++) data[i] = timepointData[i][0];
		}
		return data;
	}

	private String[] loadStringData(HDF5File hdf5File, SubWellFeature feature, int wellNr) throws IOException {
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
//				cache.removeData(well, feature);
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
