package eu.openanalytics.phaedra.silo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.SecurityService.Action;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.silo.util.ISiloStructureVisitor;
import eu.openanalytics.phaedra.silo.util.SiloModificationTransaction;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.silo.util.SiloStructureUtils;
import eu.openanalytics.phaedra.silo.util.SiloStructureVisitors.DataGroupChangedVisitor;
import eu.openanalytics.phaedra.silo.util.SiloStructureVisitors.DatasetChangedVisitor;
import eu.openanalytics.phaedra.silo.vo.Silo;

/**
 * <p>
 * This service manages the contents of silo data files.
 * </p><p>
 * A silo data file consists of zero or more data groups.
 * Each data group contains at least one dataset, which is the equivalent of one column (or 1D vector).
 * </p><p>
 * The structure of a silo data file can be retrieved via getSiloStructure().
 * This SiloStructure object is automatically updated whenever the silo structure changes.
 * </p><p>
 * This service does NOT contain any logic regarding Well or Subwell silos. That is handled by ISiloAccessor.
 * In fact, this service should never be called directly: the ISiloAccessor should be the only one making calls to this service.
 * </p>
 */
public class SiloDataService {

	private static SiloDataService instance;

	private ICache cache;
	private Map<Silo, SiloStructure> siloStructures;

	public SiloDataService() {
		// Hidden constructor.
		cache = CacheService.getInstance().createCache("SiloDataCache");
		siloStructures = new HashMap<>();
	}

	public static synchronized SiloDataService getInstance() {
		if (instance == null) {
			instance = new SiloDataService();
		}
		return instance;
	}

	public SiloStructure getSiloStructure(Silo silo) throws SiloException {
		SiloStructure root = siloStructures.get(silo);
		if (root == null) {
			root = loadStructure(silo);
			siloStructures.put(silo, root);
		}
		return root;
	}

	public SiloDataType getDataType(Silo silo, String path, String dataset) throws SiloException {
		SiloDataCacheEntry entry = getEntry(silo, path, dataset);
		return entry.dataType;
	}

	/*
	 * Read data API
	 */

	public String getSiloFSPath(Silo silo) {
		StringBuilder sb = new StringBuilder();
		sb.append("/silo.data/");
		sb.append(silo.getProtocolClass().getId());
		sb.append("/");
		sb.append(silo.getId() + ".h5");
		return sb.toString();
	}
	
	public float[] readFloatData(Silo silo, String path, String dataset) throws SiloException {
		return (float[]) getEntryData(silo, path, dataset);
	}

	public String[] readStringData(Silo silo, String path, String dataset) throws SiloException {
		return (String[]) getEntryData(silo, path, dataset);
	}

	public int[] readIntData(Silo silo, String path, String dataset) throws SiloException {
		return (int[]) getEntryData(silo, path, dataset);
	}

	public long[] readLongData(Silo silo, String path, String dataset) throws SiloException {
		return (long[]) getEntryData(silo, path, dataset);
	}

	public double[] readDoubleData(Silo silo, String path, String dataset) throws SiloException {
		return (double[]) getEntryData(silo, path, dataset);
	}

	/*
	 * Add/update data API
	 */

	public void replaceData(Silo silo, String path, String dataset, Object values) throws SiloException {
		setEntryData(silo, path, dataset, values);
	}

	public void expandData(Silo silo, String path, String dataset, int elementsToAdd) throws SiloException {
		SiloDataCacheEntry entry = getEntry(silo, path, dataset);

		Object newArray = null;
		if (entry.dataType == SiloDataType.None) {
			// There is nothing to expand.
		} else if (entry.dataType == SiloDataType.Float) {
			newArray = Arrays.copyOf(entry.floatData, entry.floatData.length + elementsToAdd);
		} else if (entry.dataType == SiloDataType.String) {
			newArray = Arrays.copyOf(entry.stringData, entry.stringData.length + elementsToAdd);
		} else if (entry.dataType == SiloDataType.Integer) {
			newArray = Arrays.copyOf(entry.integerData, entry.integerData.length + elementsToAdd);
		} else if (entry.dataType == SiloDataType.Long) {
			newArray = Arrays.copyOf(entry.longData, entry.longData.length + elementsToAdd);
		} else if (entry.dataType == SiloDataType.Double) {
			newArray = Arrays.copyOf(entry.doubleData, entry.doubleData.length + elementsToAdd);
		}

		replaceData(silo, path, dataset, newArray);
	}

	public void shrinkData(Silo silo, String path, String dataset, int[] elementsToRemove) throws SiloException {
		SiloDataCacheEntry entry = getEntry(silo, path, dataset);

		Object newArray = null;
		if (entry.dataType == SiloDataType.None) {
			// There is nothing to shrink.
		} else if (entry.dataType == SiloDataType.Float) {
			newArray = CollectionUtils.removeElements(entry.floatData, elementsToRemove);
		} else if (entry.dataType == SiloDataType.String) {
			newArray = CollectionUtils.removeElements(entry.stringData, elementsToRemove);
		} else if (entry.dataType == SiloDataType.Integer) {
			newArray = CollectionUtils.removeElements(entry.integerData, elementsToRemove);
		} else if (entry.dataType == SiloDataType.Long) {
			newArray = CollectionUtils.removeElements(entry.longData, elementsToRemove);
		} else if (entry.dataType == SiloDataType.Double) {
			newArray = CollectionUtils.removeElements(entry.doubleData, elementsToRemove);
		}

		replaceData(silo, path, dataset, newArray);
	}

	public void saveSiloChanges(Silo silo, IProgressMonitor monitor) throws SiloException {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, silo);

		if (monitor == null) monitor = new NullProgressMonitor();

		// Get a collection of all datasets to save (modified or not).
		Set<SiloStructure> allDatasets = SiloStructureUtils.getAllDataSets(getSiloStructure(silo));
		if (allDatasets.isEmpty()) return;

		if (monitor.isCanceled()) return;
		monitor.beginTask("Saving silo", allDatasets.size());

		SiloModificationTransaction transaction = new SiloModificationTransaction(silo);
		try {
			transaction.smartBegin();

			for (SiloStructure ds: allDatasets) {
				if (monitor.isCanceled()) return;
				monitor.subTask("Saving dataset " + ds.getName());
				SiloDataCacheEntry entry = getEntry(silo, ds.getPath(), ds.getName());
				saveData(entry, transaction);
				monitor.worked(1);
			}

			if (monitor.isCanceled()) return;
			transaction.commit();
		} catch (IOException e) {
			throw new SiloException("Failed to save silo changes", e);
		} finally {
			transaction.rollback();
		}

		monitor.done();
	}

	public void revertSiloChanges(Silo silo) throws SiloException {
		// Step 1: Revert all datasets for the current (modified) silo structure
		Set<SiloStructure> allDatasets = SiloStructureUtils.getAllDataSets(getSiloStructure(silo));
		for (SiloStructure dataset : allDatasets) {
			CacheKey key = getKey(silo.getId(), dataset.getPath(), dataset.getName());
			if (cache.contains(key)) {
				cache.remove(key);
			}
		}

		// Step 2: Revert the silo structure
		siloStructures.remove(silo);

		// Step 3: Revert all datasets for the original silo structure
		allDatasets = SiloStructureUtils.getAllDataSets(getSiloStructure(silo));
		for (SiloStructure dataset : allDatasets) {
			CacheKey key = getKey(silo.getId(), dataset.getPath(), dataset.getName());
			if (cache.contains(key)) {
				cache.remove(key);
			}
		}
	}

	/*
	 * Data file management
	 * ********************
	 */

	public void createDataFile(Silo silo) throws IOException {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, silo);

		// If file exists, do nothing.
		String hdf5Path = getSiloFSPath(silo);
		if (Screening.getEnvironment().getFileServer().exists(hdf5Path)) return;

		// Create an HDF5 File for this silo.
		String tempFile = FileUtils.generateTempFolder(true) + "/" + silo.getId() + ".h5";
		HDF5File dataFile = new HDF5File(tempFile, false);
		dataFile.close();
		Screening.getEnvironment().getFileServer().putContents(hdf5Path, new File(tempFile));
	}

	/**
	 * Copy the data file from silo to siloCopy. Useful in a duplicate method.
	 *
	 * @param silo Silo from which we would like to copy the data file.
	 * @param siloCopy Silo to which we would like to copy the data file.
	 * @throws IOException
	 */
	public void copyDataFile(Silo silo, Silo siloCopy) throws IOException {
		// Check for read access for the Silo we would like to copy and write access for the Silo to which we want to copy.
		SecurityService.getInstance().checkPersonalObjectWithException(Action.READ, silo);
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, siloCopy);
		String hdf5Path = SiloDataService.getInstance().getSiloFSPath(silo);
		String hdf5PathCopy = SiloDataService.getInstance().getSiloFSPath(siloCopy);
		Screening.getEnvironment().getFileServer().copy(hdf5Path, hdf5PathCopy);
	}

	public void deleteDataFile(Silo silo) throws IOException {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.DELETE, silo);
		String hdf5Path = SiloDataService.getInstance().getSiloFSPath(silo);
		Screening.getEnvironment().getFileServer().delete(hdf5Path);
	}

	/*
	 * Non-public
	 * **********
	 */

	private Object getEntryData(Silo silo, String path, String dataset) throws SiloException {
		SiloDataCacheEntry entry = getEntry(silo, path, dataset);

		switch (entry.dataType) {
		case None:
			return null;
		case Float:
			return entry.floatData;
		case String:
			return entry.stringData;
		case Integer:
			return entry.integerData;
		case Long:
			return entry.longData;
		case Double:
			return entry.doubleData;
		}
		return null;
	}

	private void setEntryData(Silo silo, String path, String dataset, Object data) throws SiloException {
		SiloDataCacheEntry entry = getEntry(silo, path, dataset);
		if (data == null) {
			// Setting to null means removing the dataset.
			entry.clear();
			// Update the silo structure accordingly.
			getSiloStructure(silo).receive(new DatasetChangedVisitor(path, dataset, 0));
		} else {
			// Modify the entry (existing or new)
			boolean isNewDataSet = (entry.dataType == SiloDataType.None);
			entry.clear();

			if (data instanceof float[]) {
				entry.floatData = (float[])data;
				entry.dataType = SiloDataType.Float;
			} else if (data instanceof String[]) {
				entry.stringData = (String[])data;
				entry.dataType = SiloDataType.String;
			} else if (data instanceof int[]) {
				entry.integerData = (int[])data;
				entry.dataType = SiloDataType.Integer;
			} else if (data instanceof long[]) {
				entry.longData = (long[])data;
				entry.dataType = SiloDataType.Long;
			} else if (data instanceof double[]) {
				entry.doubleData = (double[])data;
				entry.dataType = SiloDataType.Double;
			} else {
				throw new SiloException("Unsupported data type: " + data.getClass());
			}

			if (isNewDataSet) {
				boolean isNewDataGroup = !getSiloStructure(silo).getDataGroups().contains(path);
				if (isNewDataGroup) getSiloStructure(silo).receive(new DataGroupChangedVisitor(path));
			}

			// Update the silo structure for the modified/added dataset
			getSiloStructure(silo).receive(new DatasetChangedVisitor(path, dataset, CollectionUtils.getSize(data)));
		}
	}

	private SiloDataCacheEntry getEntry(Silo silo, String path, String dataset) throws SiloException {
		CacheKey key = getKey(silo.getId(), path, dataset);
		if (cache.contains(key)) {
			return (SiloDataCacheEntry) cache.get(key);
		} else {
			// Create entry, load data, and cache it.
			SiloDataCacheEntry entry = new SiloDataCacheEntry();
			entry.silo = silo;
			entry.path = path;
			entry.dataset = dataset;
			entry.dataType = SiloDataType.None;
			loadData(entry);
			cache.put(key, entry);
			return entry;
		}
	}

	private void loadData(SiloDataCacheEntry entry) throws SiloException {
		HDF5File file = null;
		try {
			file = getDataFile(entry.silo);
			String path = entry.getHDF5Path();
			if (!file.exists(path)) return;
			if (file.isCompoundDataSet(path)) return;

			Object data = file.getAnyData1D(path, 1);
			if (data == null) return;
			else if (data instanceof float[]) {
				entry.floatData = (float[])data;
				entry.dataType = SiloDataType.Float;
			} else if (data instanceof String[]) {
				entry.stringData = (String[])data;
				entry.dataType = SiloDataType.String;
			} else if (data instanceof int[]) {
				entry.integerData = (int[])data;
				entry.dataType = SiloDataType.Integer;
			} else if (data instanceof long[]) {
				entry.longData = (long[])data;
				entry.dataType = SiloDataType.Long;
			} else if (data instanceof double[]) {
				entry.doubleData = (double[])data;
				entry.dataType = SiloDataType.Double;
			}
		} catch (IOException e) {
			throw new SiloException("Failed to retrieve dataset: " + entry.dataset, e);
		} finally {
			file.close();
		}
	}

	private void saveData(SiloDataCacheEntry entry, SiloModificationTransaction transaction) throws SiloException {
		if (transaction == null) throw new SiloException("No silo modification transaction active");
		HDF5File file = transaction.getWorkingCopy();

		try {
			// Create the parent path if needed.
			String path = entry.getHDF5Path();
			String parentPath = FileUtils.getPath(path);
			if (!file.exists(parentPath)) file.createGroup(parentPath);

			switch (entry.dataType) {
			case None:
				// Write nothing.
				break;
			case Float:
				file.writeNumericData(path, entry.floatData);
				break;
			case String:
				file.writeStringData1D(path, entry.stringData);
				break;
			case Integer:
				file.writeNumericData(path, entry.integerData);
				break;
			case Long:
				file.writeNumericData(path, entry.longData);
				break;
			case Double:
				file.writeNumericData(path, entry.doubleData);
				break;
			}
		} catch (IOException e) {
			throw new SiloException("Failed to write dataset: " + entry.dataset, e);
		}
	}

	private SiloStructure loadStructure(Silo silo) throws SiloException {
		final SiloStructure root = new SiloStructure(silo, null);
		root.setPath("/");
		root.setName("");
		HDF5File file = null;
		try {
			file = getDataFile(silo);
			fillStructure(root, file);
		} catch (IOException e) {
			throw new SiloException("Failed to retrieve silo structure", e);
		} finally {
			if (file != null) file.close();
		}

		Set<SiloStructure> dataGroups = SiloStructureUtils.getAllDataGroups(root);
		for (SiloStructure struct: dataGroups) root.getDataGroups().add(struct.getFullName());

		root.receive(new ISiloStructureVisitor() {
			@Override
			public void visit(SiloStructure structure) {
				if (structure.isDataset()) {
					String dataGroup = structure.getPath();
					root.addDataSet(dataGroup, structure.getName());
					root.setDataSetSize(dataGroup, structure.getDatasetSize());
				}
			}
		});

		return root;
	}

	private void fillStructure(SiloStructure structure, HDF5File file) throws IOException {
		String path = structure.getPath();
		String fullName = path + (path.endsWith("/") ? "" : "/") + escape(structure.getName());
		if (file.isGroup(fullName)) {
			structure.setDataset(false);
			String[] children = file.getChildren(fullName);
			for (String child : children) {
				SiloStructure childStructure = new SiloStructure(structure);
				childStructure.setPath(fullName);
				childStructure.setName(unescape(child));
				fillStructure(childStructure, file);
				structure.getChildren().add(childStructure);
			}
		} else {
			structure.setDataset(true);
			long[] dims = file.getDataDimensions(fullName);
			structure.setDatasetSize(dims[0]);
		}
	}

	private HDF5File getDataFile(Silo silo) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.READ, silo);
		return HDF5File.openForRead(getSiloFSPath(silo));
	}

	private static String escape(String string) {
		return string.replace("/", "%2F");
	}

	private static String unescape(String string) {
		return string.replace("%2F", "/");
	}

	private static CacheKey getKey(long siloId, String datasetPath, String datasetName) {
		return CacheKey.create(siloId, datasetPath, datasetName);
	}
	
	/*
	 * Nested classes
	 * **************
	 */

	public static enum SiloDataType {
		None,
		String,
		Float,
		Double,
		Integer,
		Long
	}

	private static class SiloDataCacheEntry {
		public Silo silo;
		public String path;
		public String dataset;

		public SiloDataType dataType;

		public String[] stringData;
		public float[] floatData;
		public double[] doubleData;
		public int[] integerData;
		public long[] longData;

		public void clear() {
			dataType = SiloDataType.None;
			stringData = null;
			floatData = null;
			doubleData = null;
			integerData = null;
			longData = null;
		}

		public String getHDF5Path() {
			return path + (path.endsWith("/") ? "" : "/") + escape(dataset);
		}
	}

}