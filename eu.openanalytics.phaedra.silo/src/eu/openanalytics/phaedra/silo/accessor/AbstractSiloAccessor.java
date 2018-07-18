package eu.openanalytics.phaedra.silo.accessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.silo.SiloDataService;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatapoint;
import eu.openanalytics.phaedra.silo.dao.SiloDatasetData.SiloDatasetColumnData;
import eu.openanalytics.phaedra.silo.util.ObjectCopyFactory;
import eu.openanalytics.phaedra.silo.util.SiloUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public abstract class AbstractSiloAccessor<T> implements ISiloAccessor<T> {

	private Silo originalSilo;
	private Silo workingCopySilo;
	private Map<String, SiloDatasetData> workingCopyData;
	private Map<String, List<T>> rowObjectCache;
	private boolean dirty;
	
	public AbstractSiloAccessor(Silo silo) {
		originalSilo = silo;
		init();
	}
	
	@Override
	public Silo getSilo() {
		return workingCopySilo;
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public SiloDataset createDataset(String datasetName) throws SiloException {
		if (datasetName == null || datasetName.isEmpty()) throw new SiloException("Cannot create dataset: name must not be empty");
		SiloDataset existingDs = getDataset(datasetName, false);
		if (existingDs != null) throw new SiloException("Cannot create dataset: a dataset with name " + datasetName + " already exists");
		
		SiloDataset ds = new SiloDataset();
		ds.setName(datasetName);
		ds.setSilo(workingCopySilo);
		ds.setColumns(new ArrayList<>());
		workingCopySilo.getDatasets().add(ds);
		
		dirty = true;
		notifySiloChanged();
		return ds;
	}

	@Override
	public void removeDataset(String datasetName) throws SiloException {
		SiloDataset dsToRemove = getDataset(datasetName, true);
		// Note: cannot use SiloDataset.equals() because its id may be 0
		boolean dsRemoved = false;
		for (int i = 0; i < workingCopySilo.getDatasets().size(); i++) {
			if (workingCopySilo.getDatasets().get(i) == dsToRemove) {
				workingCopySilo.getDatasets().remove(i);
				dsRemoved = true;
				break;
			}
		}
		if (dsRemoved) {
			dirty = true;
			notifySiloChanged();
		}
	}

	@Override
	public SiloDatasetColumn createColumn(String datasetName, String columnName, SiloDataType dataType) throws SiloException {
		SiloDatasetColumn existingCol = getColumn(datasetName, columnName, false);
		if (existingCol != null) throw new SiloException("Cannot create column: a column with name " + columnName + " already exists");
		
		SiloDataset dataset = getDataset(datasetName, true);
		SiloDatasetColumn column = new SiloDatasetColumn();
		column.setName(columnName);
		column.setType(dataType);
		column.setDataset(dataset);
		dataset.getColumns().add(column);

		SiloDatasetData data = getWorkingCopyData(datasetName);
		SiloDatasetColumnData columnData = SiloUtils.createColumnData(column, data.getDataPoints().length);
		data.getColumnData().put(column.getName(), columnData);
		
		setDefaultValues(column, columnData, null);
		
		dirty = true;
		notifySiloChanged();
		return column;
	}

	@Override
	public void removeColumn(String datasetName, String columnName) throws SiloException {
		SiloDatasetColumn colToRemove = getColumn(datasetName, columnName, true);
		SiloDataset ds = getDataset(datasetName, true);
		// Note: cannot use SiloDatasetColumn.equals() because its id may be 0
		boolean colRemoved = false;
		for (int i = 0; i < ds.getColumns().size(); i++) {
			if (ds.getColumns().get(i) == colToRemove) {
				ds.getColumns().remove(i);
				colRemoved = true;
				break;
			}
		}
		if (colRemoved) {
			dirty = true;
			notifySiloChanged();
		}
	}

	@Override
	public SiloDataType getColumnDataType(String datasetName, String columnName) throws SiloException {
		SiloDatasetColumn column = getColumn(datasetName, columnName, true);
		return column.getType();
	}

	@Override
	public int getRowCount(String datasetName) throws SiloException {
		SiloDatasetData data = getWorkingCopyData(datasetName);
		return (data == null) ? 0 : data.getDataPoints().length;
	}

	@Override
	public T getRowObject(String datasetName, int rowIndex) throws SiloException {
		List<T> rows = getRows(datasetName);
		if (rows.size() <= rowIndex) return null;
		return rows.get(rowIndex);
	}

	@Override
	public int getIndexOfRow(String datasetName, T rowObject) throws SiloException {
		List<T> rows = getRows(datasetName);
		return rows.indexOf(rowObject);
	}
	
	@Override
	public Iterator<T> getRowIterator(String datasetName) throws SiloException {
		return getRows(datasetName).iterator();
	}

	@Override
	public void addRows(String datasetName, T[] rows) throws SiloException {
		if (rows == null || rows.length == 0) return;
		
		List<T> allRows = getRows(datasetName);
		List<T> newRows = new ArrayList<>();
		Set<T> currentRows = new HashSet<>(allRows);
		for (T row: rows) {
			if (!currentRows.contains(row)) {
				allRows.add(row);
				newRows.add(row);
			}
		}
		if (newRows.isEmpty()) return;
		
		SiloDataset ds = getDataset(datasetName, true);
		SiloDatasetData data = getWorkingCopyData(datasetName);
		
		// First, add new datapoints corresponding to the new rows.
		SiloDatapoint[] points = data.getDataPoints();
		int originalLength = points.length;
		points = Arrays.copyOf(points, originalLength + newRows.size());
		for (int i=0; i<newRows.size(); i++) {
			points[originalLength + i] = createDataPoint(newRows.get(i));
		}
		data.setDataPoints(points);
		
		// Then, update all columns accordingly.
		for (SiloDatasetColumn column: ds.getColumns()) {
			SiloDatasetColumnData columnData = getWorkingCopyColumnData(datasetName, column.getName());
			if (columnData == null) {
				columnData = SiloUtils.createColumnData(column, points.length);
				data.getColumnData().put(column.getName(), columnData);
			} else {
				SiloUtils.resizeColumnData(columnData, points.length);
			}
			
			setDefaultValues(column, columnData, newRows);
		}

		dirty = true;
		notifySiloChanged();
	}

	@Override
	public void removeRows(String datasetName, int[] rows) throws SiloException {
		if (rows != null && rows.length == 0) return;
		
		SiloDataset ds = getDataset(datasetName, true);
		SiloDatasetData data = getWorkingCopyData(datasetName);
		
		int size = data.getDataPoints().length;
		if (size == 0) return;
		
		if (rows == null || rows.length == size) {
			// Delete all rows
			getRows(datasetName).clear();
			data.setDataPoints(new SiloDatapoint[0]);
			for (SiloDatasetColumn column: ds.getColumns()) {
				SiloDatasetColumnData columnData = getWorkingCopyColumnData(datasetName, column.getName());
				if (columnData != null) SiloUtils.resizeColumnData(columnData, 0);
			}
		} else {
			List<T> rowList = getRows(datasetName);
			int[] rowsToKeep = IntStream.range(0, rowList.size())
					.filter(i -> !CollectionUtils.contains(rows, i))
					.toArray();
			
			List<T> rowListFiltered = Arrays.stream(rowsToKeep).mapToObj(i -> rowList.get(i)).collect(Collectors.toList());
			rowList.clear();
			rowList.addAll(rowListFiltered);
			data.setDataPoints(Arrays.stream(rowsToKeep).mapToObj(i -> data.getDataPoints()[i]).toArray(i -> new SiloDatapoint[i]));

			for (SiloDatasetColumn column: ds.getColumns()) {
				SiloDatasetColumnData columnData = getWorkingCopyColumnData(datasetName, column.getName());
				if (columnData != null) SiloUtils.resizeColumnData(columnData, rows);
			}
		}
		
		dirty = true;
		notifySiloChanged();
	}

	@Override
	public float[] getFloatValues(String datasetName, String columnName) throws SiloException {
		SiloDatasetColumnData columnData = getWorkingCopyColumnData(datasetName, columnName);
		return (columnData == null) ? null : columnData.getFloatData();
	}

	@Override
	public long[] getLongValues(String datasetName, String columnName) throws SiloException {
		SiloDatasetColumnData columnData = getWorkingCopyColumnData(datasetName, columnName);
		return (columnData == null) ? null : columnData.getLongData();
	}

	@Override
	public String[] getStringValues(String datasetName, String columnName) throws SiloException {
		SiloDatasetColumnData columnData = getWorkingCopyColumnData(datasetName, columnName);
		return (columnData == null) ? null : columnData.getStringData();
	}

	@Override
	public void updateValues(String datasetName, String columnName, Object newData) throws SiloException {
		SiloDatasetData data = getWorkingCopyData(datasetName);
		SiloDatasetColumnData columnData = getWorkingCopyColumnData(datasetName, columnName);
		if (columnData == null) {
			SiloDatasetColumn column = getColumn(datasetName, columnName, true);
			columnData = SiloUtils.createColumnData(column, 0);
			data.getColumnData().put(column.getName(), columnData);
		}
		SiloUtils.replaceColumnData(columnData, newData, data.getDataPoints().length);
		
		dirty = true;
		notifySiloChanged();
	}

	@Override
	public void save(IProgressMonitor monitor) throws SiloException {
		// Save changes to the model (new datasets, new columns etc).
		SiloUtils.saveSiloChanges(originalSilo, workingCopySilo);
		// Copy back, to take into account newly generated IDs.
		ObjectCopyFactory.copy(originalSilo, workingCopySilo, true);
		// Save all modified dataset contents
		SiloDatasetData[] data = workingCopyData.values().toArray(new SiloDatasetData[workingCopyData.size()]);
		SiloDataService.getInstance().saveData(data, monitor);
		dirty = false;
		notifySiloChanged();
	}

	@Override
	public void revert() {
		init();
		notifySiloChanged();
	}

	protected void init() {
		workingCopySilo = new Silo();
		workingCopyData = new HashMap<>();
		rowObjectCache = new HashMap<>();
		dirty = false;
		ObjectCopyFactory.copy(originalSilo, workingCopySilo, true);
	}
	
	protected SiloDataset getDataset(String datasetName, boolean required) throws SiloException {
		SiloDataset dataset = null;
		if (datasetName != null && !datasetName.isEmpty()) {
			dataset = SiloService.streamableList(workingCopySilo.getDatasets()).stream()
					.filter(ds -> ds.getName().equals(datasetName)).findAny().orElse(null);
		}
		if (required && dataset == null) throw new SiloException("Dataset does not exist: " + datasetName);
		return dataset;
	}
	
	protected SiloDatasetColumn getColumn(String datasetName, String columnName, boolean required) throws SiloException {
		SiloDataset dataset = getDataset(datasetName, required);
		if (dataset == null) return null;
		SiloDatasetColumn column = null;
		if (columnName != null && !columnName.isEmpty()) {
			column = SiloService.streamableList(dataset.getColumns()).stream()
					.filter(col -> col.getName().equals(columnName)).findAny().orElse(null);
		}
		if (required && column == null) throw new SiloException("Column does not exist: " + columnName);
		return column;
	}

	protected SiloDatasetData getWorkingCopyData(String datasetName) throws SiloException {
		if (datasetName == null || datasetName.isEmpty()) return null;
		if (workingCopyData.containsKey(datasetName)) return workingCopyData.get(datasetName);
		SiloDatasetData data = SiloDataService.getInstance().getData(workingCopySilo, datasetName);
		SiloDatasetData copyOfData = (data == null) ? null : data.copy();
		workingCopyData.put(datasetName, copyOfData);
		return copyOfData;
	}
	
	protected SiloDatasetColumnData getWorkingCopyColumnData(String datasetName, String columnName) throws SiloException {
		if (columnName == null || columnName.isEmpty()) return null;
		SiloDatasetData data = getWorkingCopyData(datasetName);
		if (data == null) return null;
		SiloDatasetColumnData columnData = data.getColumnData().get(columnName);
		return columnData;
	}
	
	protected List<T> getRows(String datasetName) throws SiloException {
		List<T> rows = rowObjectCache.get(datasetName);
		if (rows == null) {
			rows = loadRowObjects(datasetName);
			if (rows == null) rows = new ArrayList<>();
			rowObjectCache.put(datasetName, rows);
		}
		return rows;
	}
	
	protected void notifySiloChanged() {
		ModelEvent event = new ModelEvent(getSilo(), ModelEventType.ObjectChanged, 0);
		ModelEventService.getInstance().fireEvent(event);
	}

	protected List<Well> queryWells(SiloDatapoint[] points) {
		List<Well> wells = new ArrayList<>();
		if (points == null || points.length == 0) return wells;

		int batchSize = 200;
		int batchCount = points.length / batchSize;
		int remainder = points.length % batchSize;
		
		long[] wellIds = Arrays.stream(points).mapToLong(p -> p.getWellId()).toArray();
		for (int i=0; i<batchCount; i++) {
			queryWells(wellIds, i*batchSize, (i+1)*batchSize, wells);
		}
		if (remainder > 0) queryWells(wellIds, batchCount*batchSize, remainder+batchCount*batchSize, wells);

		return wells;
	}
	
	private void queryWells(long[] ids, int from, int to, List<Well> results) {
		StringBuilder sb = new StringBuilder();
		for (int i=from; i<to; i++) sb.append(ids[i] + ",");
		sb.deleteCharAt(sb.length()-1);
		String jpql = "select w from Well w where w.id in (" + sb.toString() + ")";

		EntityManager em = Screening.getEnvironment().getEntityManager();
		Query query = em.createQuery(jpql);
		List<?> resultSet = JDBCUtils.queryWithLock(query, em);

		for (int i=from; i<to; i++) {
			for (Object res: resultSet) {
				Well well = (Well)res;
				if (ids[i] == well.getId()) results.add(well);
			}
		}
	}
	
	/**
	 * A dataset is accessed for the first time. Load the objects that represent the rows of the dataset.
	 *
	 * @param datasetName The dataset that is being accessed.
	 * @return The objects (e.g. Wells) that correspond to the rows of the dataset.
	 * @throws SiloException If the row objects cannot be loaded.
	 */
	protected abstract List<T> loadRowObjects(String datasetName) throws SiloException;
	
	/**
	 * Set default values for the given rows in the given column.
	 * 
	 * @param column The column of the dataset to update.
	 * @param columnData The current data of the column, with room for the new rows.
	 * @param newRows The new rows being appended to the column, or null if the column is new.
	 * @throws SiloException If the column values cannot be updated for any reason.
	 */
	protected abstract void setDefaultValues(SiloDatasetColumn column, SiloDatasetColumnData columnData, List<T> newRows) throws SiloException;
	
	/**
	 * Create a new datapoint for the given row.
	 * 
	 * @param row The new row to create a datapoint for.
	 * @return A new datapoint that can be inserted in a silo dataset.
	 */
	protected abstract SiloDatapoint createDataPoint(T row);
}
