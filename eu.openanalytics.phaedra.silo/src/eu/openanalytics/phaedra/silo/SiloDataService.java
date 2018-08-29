package eu.openanalytics.phaedra.silo;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.SecurityService.Action;
import eu.openanalytics.phaedra.silo.dao.SiloDataDAO;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatasetColumnData;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;

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
 * In fact, this service should never be called directly: always use a ISiloAccessor to manipulate silo data.
 * </p>
 */
public class SiloDataService {

	private static SiloDataService instance = new SiloDataService();

	private SiloDataDAO dao;
	private ICache cache;

	public SiloDataService() {
		// Hidden constructor.
		dao = new SiloDataDAO();
		cache = CacheService.getInstance().createCache("SiloDataCache");
	}

	public static SiloDataService getInstance() {
		return instance;
	}

	public SiloDataType getDataType(Silo silo, String datasetName, String columnName) throws SiloException {
		SiloDatasetData data = getData(silo, datasetName);
		if (data == null) return SiloDataType.None;
		SiloDatasetColumnData columnData = data.getColumnData().get(columnName);
		if (columnData == null) return SiloDataType.None;
		return columnData.getColumn().getType();
	}

	public String[] readStringData(Silo silo, String datasetName, String columnName) throws SiloException {
		return (String[]) readData(silo, datasetName, columnName);
	}

	public float[] readFloatData(Silo silo, String datasetName, String columnName) throws SiloException {
		return (float[]) readData(silo, datasetName, columnName);
	}

	public long[] readLongData(Silo silo, String datasetName, String columnName) throws SiloException {
		return (long[]) readData(silo, datasetName, columnName);
	}
	
	public Object readData(Silo silo, String datasetName, String columnName) throws SiloException {
		SiloDatasetData data = getData(silo, datasetName);
		if (data == null) return null;
		
		SiloDatasetColumnData colData = data.getColumnData().get(columnName);
		if (colData == null) return null;
		
		switch (colData.getColumn().getType()) {
		case String:
			return colData.getStringData();
		case Float:
			return colData.getFloatData();
		case Long:
			return colData.getLongData();
		default:
			return null;
		}
	}

	public void saveData(SiloDatasetData[] dataSets, IProgressMonitor monitor) throws SiloException {
		if (dataSets == null || dataSets.length == 0) return;
		Silo silo = dataSets[0].getDataset().getSilo();
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, silo);

		if (monitor == null) monitor = new NullProgressMonitor();
		if (monitor.isCanceled()) return;
		
		monitor.beginTask("Saving silo data", dataSets.length);
		for (SiloDatasetData data: dataSets) {
			if (monitor.isCanceled()) return;
			dao.saveData(data);
			cache.put(getKey(silo.getId(), data.getDataset().getName()), data);
			monitor.worked(1);
		}
		monitor.done();
	}

	/**
	 * Copy all data from one silo to another.
	 *
	 * @param from The source silo
	 * @param to The destination silo
	 */
	public void copyData(Silo from, Silo to) {
		// Check for read access for the Silo we would like to copy and write access for the Silo to which we want to copy.
		SecurityService.getInstance().checkPersonalObjectWithException(Action.READ, from);
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, to);

		for (SiloDataset dsFrom: from.getDatasets()) {
			SiloDataset dsTo = SiloService.streamableList(to.getDatasets()).stream().filter(ds -> ds.getName().equals(dsFrom.getName())).findAny().orElse(null);
			if (dsTo == null) continue;
			SiloDatasetData dataFrom = getData(from, dsFrom.getName());
			if (dataFrom == null) continue;
			SiloDatasetData dataTo = dataFrom.copy();
			dataTo.setDataset(dsTo);
			dao.saveData(dataTo);
			cache.put(getKey(to.getId(), dataTo.getDataset().getName()), dataTo);
		}
	}

	public void deleteData(Silo silo) {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.DELETE, silo);
		for (SiloDataset ds: silo.getDatasets()) {
			dao.deleteData(ds.getId());
		}
	}

	public SiloDatasetData getData(Silo silo, String datasetName) {
		CacheKey key = getKey(silo.getId(), datasetName);
		if (cache.contains(key)) {
			return (SiloDatasetData) cache.get(key);
		} else {
			SiloDataset dataset = SiloService.streamableList(silo.getDatasets()).stream().filter(ds -> ds.getName().equals(datasetName)).findAny().orElse(null);
			SiloDatasetData data = null;
			if (dataset != null) data = dao.loadData(dataset);
			cache.put(key, data);
			return data;
		}
	}
	
	public int getDataSize(Silo silo, String datasetName) {
		SiloDataset dataset = SiloService.streamableList(silo.getDatasets()).stream().filter(ds -> ds.getName().equals(datasetName)).findAny().orElse(null);
		if (dataset == null) return 0;
		return dao.getDataSize(dataset);
	}

	private static CacheKey getKey(long siloId, String datasetName) {
		return CacheKey.create(siloId, datasetName);
	}
	
	public static enum SiloDataType {
		None,
		String,
		Float,
		Long
	}

}