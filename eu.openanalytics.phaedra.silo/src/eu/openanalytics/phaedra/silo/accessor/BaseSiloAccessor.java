package eu.openanalytics.phaedra.silo.accessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;

import eu.openanalytics.phaedra.base.db.JDBCUtils;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.silo.SiloDataService;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.util.ColumnDescriptor;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.silo.util.SiloStructureUtils;
import eu.openanalytics.phaedra.silo.vo.Silo;

public abstract class BaseSiloAccessor<T> implements ISiloAccessor<T> {

	private Silo silo;
	private Map<String, List<T>> rowsPerGroup;

	public BaseSiloAccessor(Silo silo) {
		this.silo = silo;
		this.rowsPerGroup = new HashMap<>();
	}

	@Override
	public Silo getSilo() {
		return silo;
	}

	@Override
	public SiloStructure getSiloStructure() throws SiloException {
		return SiloDataService.getInstance().getSiloStructure(silo);
	}

	@Override
	public String[] getDataGroups() throws SiloException {
		List<String> groups = getSiloStructure().getDataGroups();
		return groups.toArray(new String[groups.size()]);
	}

	@Override
	public String[] getColumns(String dataGroup) throws SiloException {
		String[] sets = getSiloStructure().getDataSets(dataGroup);
		if (sets == null) sets = new String[0];
		return sets;
	}

	@Override
	public int getRowCount(String dataGroup) throws SiloException {
		return (int)getSiloStructure().getDatasetSize(dataGroup);
	}

	@Override
	public T getRow(String dataGroup, int rowIndex) throws SiloException {
		List<T> rows = getRows(dataGroup);
		return rows.get(rowIndex);
	}

	@Override
	public int getRow(String dataGroup, T object) throws SiloException {
		return getRows(dataGroup).indexOf(object);
	}

	@Override
	public Iterator<T> getRowIterator(String dataGroup) throws SiloException {
		return getRows(dataGroup).iterator();
	}

	@Override
	public SiloDataType getDataType(String dataGroup, int column) throws SiloException {
		return SiloDataService.getInstance().getDataType(silo, dataGroup, getColumn(dataGroup, column));
	}

	@Override
	public float[] getFloatValues(String dataGroup, int column) throws SiloException {
		return SiloDataService.getInstance().readFloatData(silo, dataGroup, getColumn(dataGroup, column));
	}

	@Override
	public String[] getStringValues(String dataGroup, int column) throws SiloException {
		return SiloDataService.getInstance().readStringData(silo, dataGroup, getColumn(dataGroup, column));
	}

	@Override
	public int[] getIntValues(String dataGroup, int column) throws SiloException {
		return SiloDataService.getInstance().readIntData(silo, dataGroup, getColumn(dataGroup, column));
	}

	@Override
	public long[] getLongValues(String dataGroup, int column) throws SiloException {
		return SiloDataService.getInstance().readLongData(silo, dataGroup, getColumn(dataGroup, column));
	}

	@Override
	public double[] getDoubleValues(String dataGroup, int column) throws SiloException {
		return SiloDataService.getInstance().readDoubleData(silo, dataGroup, getColumn(dataGroup, column));
	}

	@Override
	public void addRows(String dataGroup, T[] rows, SiloStructure siloSource) throws SiloException {
		if (rows == null || rows.length == 0) return;

		List<T> newRows = new ArrayList<>();
		List<T> currentRows = getRows(dataGroup);
		// Wrap in Set for huge performance gain.
		Set<T> currentRowsSet = new HashSet<>(currentRows);
		for (T row: rows) {
			// Ignore duplicate rows
			if (currentRowsSet.add(row)) {
				currentRows.add(row);
				newRows.add(row);
			}
		}

		int offset = currentRows.size() - newRows.size();
		String[] columns = getColumns(dataGroup);
		for (int i=0; i<columns.length; i++) {
			// 1. Expand array sizes.
			SiloDataService.getInstance().expandData(silo, dataGroup, getColumn(dataGroup, i), newRows.size());
			// 2. Update arrays with default values.
			createOrUpdateColumn(dataGroup, columns[i], offset);
		}

		rowsAdded(dataGroup, newRows);

		if (siloSource != null) {
			// The new rows came from existing Silo.
			ISiloAccessor<PlatformObject> siloAccessor = SiloService.getInstance().getSiloAccessor(siloSource.getSilo());
			String srcDataGroup = siloSource.getFullName();
			String[] srcColumns = siloAccessor.getColumns(srcDataGroup);
			// Update the list of columns.
			columns = getColumns(dataGroup);
			int[] colIndexes = new int[srcColumns.length];

			// Add the columns from the source Silo to this Silo.
			int added = 0;
			for (int i = 0; i < srcColumns.length; i++) {
				colIndexes[i] = CollectionUtils.find(columns, srcColumns[i]);
				if (colIndexes[i] < 0) {
					addColumn(dataGroup, srcColumns[i]);
					colIndexes[i] = columns.length + added;
					added++;
				}
			}

			// Loop the newly added rows and add the possibly modified values from the source Silo.
			for (int i = 0; i < newRows.size(); i++) {
				int rowIndex = siloAccessor.getRow(srcDataGroup, (PlatformObject) newRows.get(i));
				for (int j = 0; j < colIndexes.length; j++) {
					int column = colIndexes[j];
					SiloDataType dataType = getDataType(dataGroup, column);
					switch (dataType) {
					case Double:
						getDoubleValues(dataGroup, column)[offset + i] = siloAccessor.getDoubleValues(srcDataGroup, j)[rowIndex];
						break;
					case Float:
						getFloatValues(dataGroup, column)[offset + i] = siloAccessor.getFloatValues(srcDataGroup, j)[rowIndex];
						break;
					case Integer:
						getIntValues(dataGroup, column)[offset + i] = siloAccessor.getIntValues(srcDataGroup, j)[rowIndex];
						break;
					case Long:
						getLongValues(dataGroup, column)[offset + i] = siloAccessor.getLongValues(srcDataGroup, j)[rowIndex];
						break;
					case String:
						getStringValues(dataGroup, column)[offset + i] = siloAccessor.getStringValues(srcDataGroup, j)[rowIndex];
						break;
					default:
						break;
					}
				}
			}
		}

		triggerModelEvent(SiloStructureUtils.findDataGroup(getSiloStructure(), dataGroup), ModelEventType.ObjectChanged, false);
	}

	@Override
	public void addRows(String dataGroup, T[] rows) throws SiloException {
		if (rows == null || rows.length == 0) return;

		List<T> newRows = new ArrayList<>();
		List<T> currentRows = getRows(dataGroup);
		// Wrap in Set for huge performance gain.
		Set<T> currentRowsSet = new HashSet<>(currentRows);
		for (T row: rows) {
			// Ignore duplicate rows
			if (currentRowsSet.add(row)) {
				currentRows.add(row);
				newRows.add(row);
			}
		}

		String[] columns = getColumns(dataGroup);
		for (int i=0; i<columns.length; i++) {
			// 1. Expand array sizes.
			SiloDataService.getInstance().expandData(silo, dataGroup, getColumn(dataGroup, i), newRows.size());
			// 2. Update arrays with default values.
			createOrUpdateColumn(dataGroup, columns[i], currentRows.size() - newRows.size());
		}

		rowsAdded(dataGroup, newRows);

		triggerModelEvent(SiloStructureUtils.findDataGroup(getSiloStructure(), dataGroup), ModelEventType.ObjectChanged, false);
	}

	@Override
	public void removeRows(String dataGroup, int[] rows) throws SiloException {
		int currentRowCount = getRowCount(dataGroup);
		if (currentRowCount == 0) return;

		SiloStructure affectedGroup = SiloStructureUtils.findDataGroup(getSiloStructure(), dataGroup);

		if (rows == null || currentRowCount == rows.length) {
			// Drop everything in this datagroup. Faster than shrinking.
			getRows(dataGroup).clear();
			String[] columns = getColumns(dataGroup);
			for (int i=0; i<columns.length; i++) {
				// Note: do not use getDataset(String, int) here, because the columns are being removed.
				SiloDataService.getInstance().replaceData(silo, dataGroup, columns[i], null);
			}
		} else {
			List<T> currentRows = getRows(dataGroup);
			// Delete from end to start, so that element shifts do not affect the remaining indices.
			Arrays.sort(rows);
			for (int i = rows.length-1; i >= 0; i--) currentRows.remove(rows[i]);
			// Shrink columns.
			String[] columns = getColumns(dataGroup);
			for (int i=0; i<columns.length; i++) {
				SiloDataService.getInstance().shrinkData(silo, dataGroup, getColumn(dataGroup, i), rows);
			}
		}

		triggerModelEvent(affectedGroup, ModelEventType.ObjectChanged, false);
	}

	@Override
	public void addColumn(String dataGroup, String columnName) throws SiloException {
		SiloDataType type = SiloDataService.getInstance().getDataType(getSilo(), dataGroup, columnName);
		if (type != SiloDataType.None) throw new SiloException("Cannot add column '" + columnName + "': it already exists in data group '" + dataGroup + "'");
		Object data = createOrUpdateColumn(dataGroup, columnName, 0);
		replaceColumn(dataGroup, columnName, data);
	}

	@Override
	public void replaceColumn(String dataGroup, String columnName, Object data) throws SiloException {
		if (columnName.contains("\\")) throw new SiloException("Illegal column name: '" + columnName + "'. The '\\' character is not allowed in column names");
		if (data == null) {
			if (!isEditable(columnName)) throw new SiloException("Cannot delete mandatory column '" + columnName + "'");
		} else {
			String[] existingSets = getSiloStructure().getDataSets(dataGroup);
			if (existingSets != null && existingSets.length > 0) {
				int rowCount = getRowCount(dataGroup);
				int dataSize = CollectionUtils.getSize(data);
				if (rowCount != dataSize) throw new SiloException("Cannot update column: column size (" + dataSize + ") does not match row count (" + rowCount + "): " + dataGroup + "/" + columnName);
			}
		}

		SiloStructure structToChange = null;
		if (data == null) structToChange = SiloStructureUtils.findDataSet(getSiloStructure(), dataGroup, columnName);
		SiloDataService.getInstance().replaceData(silo, dataGroup, columnName, data);
		if (data != null) structToChange = SiloStructureUtils.findDataSet(getSiloStructure(), dataGroup, columnName);
		triggerModelEvent(structToChange, ModelEventType.ObjectChanged, false);
	}

	@Override
	public boolean isEditable(String columnName) {
		return !CollectionUtils.contains(getMandatoryColumns(), columnName);
	}

	/**
	 * Get the names of the mandatory columns (columns that cannot be deleted).
	 *
	 * @return The names of the mandatory columns, or an empty array if there are none.
	 */
	@Override
	public abstract String[] getMandatoryColumns();

	@Override
	public void save(IProgressMonitor monitor) throws SiloException {
		SiloDataService.getInstance().saveSiloChanges(silo, monitor);
		triggerModelEvent(getSiloStructure(), ModelEventType.ObjectChanged, true);
	}

	/**
	 * Revert the changes made to the Silo by removing all the cached values.
	 */
	@Override
	public void revert() throws SiloException {
		SiloDataService.getInstance().revertSiloChanges(silo);
		rowsPerGroup.clear();
		triggerModelEvent(getSiloStructure(), ModelEventType.ObjectChanged, false);
	}

	/*
	 * Non-public
	 * **********
	 */

	/**
	 * A data group is accessed for the first time. Load the objects that represent the rows of the data group.
	 *
	 * @param dataGroup The data group that is being accessed.
	 * @return The objects (e.g. Wells) that correspond to the rows of the data group.
	 * @throws SiloException If the row objects cannot be loaded.
	 */
	protected abstract List<T> loadGroup(String dataGroup) throws SiloException;

	/**
	 * New rows have been added to a data group. Fill out any mandatory columns if needed.
	 *
	 * @param dataGroup The data group where rows have been added to.
	 * @param rows The rows that were added.
	 * @throws SiloException If the columns cannot be filled out for any reason.
	 */
	protected abstract void rowsAdded(String dataGroup, List<T> rows) throws SiloException;

	/**
	 * Insert an appropriate default value in the array of the specified column.
	 *
	 * @param row The row to update
	 * @param array The data array to insert the value in
	 * @param index The index in the data array to modify
	 * @param columnDescriptor A descriptor of the affected column
	 * @throws SiloException If the default value cannot be computed or inserted for any reason.
	 */
	protected abstract void insertDefaultValue(T row, Object array, int index, ColumnDescriptor columnDescriptor) throws SiloException;

	protected List<Well> queryWells(long[] wellIds) {
		List<Well> wells = new ArrayList<>();
		if (wellIds == null || wellIds.length == 0) return wells;

		int batchSize = 200;
		int batchCount = wellIds.length / batchSize;
		int remainder = wellIds.length % batchSize;
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

	private List<T> getRows(String dataGroup) throws SiloException {
		List<T> rows = rowsPerGroup.get(dataGroup);
		if (rows == null) {
			rows = loadGroup(dataGroup);
			if (rows == null) rows = new ArrayList<>();
			rowsPerGroup.put(dataGroup, rows);
		}
		return rows;
	}

	private Object createOrUpdateColumn(String dataGroup, String columnName, int offset) throws SiloException {
		int colIndex = CollectionUtils.find(getColumns(dataGroup), columnName);
		SiloDataType dataType = (colIndex == -1) ? SiloDataType.None : getDataType(dataGroup, colIndex);

		ColumnDescriptor columnDescriptor = ColumnDescriptor.createDescriptor(columnName, silo);
		int rowCount = getRowCount(dataGroup);
		Object array = null;

		if (dataType == SiloDataType.None) {
			// Generate a new array
			if (columnDescriptor.feature == null || columnDescriptor.feature.isNumeric()) array = new double[rowCount];
			else array = new String[rowCount];
		} else {
			// Reuse an existing array
			if (dataType == SiloDataType.Float) array = getFloatValues(dataGroup, colIndex);
			else if (dataType == SiloDataType.Double) array = getDoubleValues(dataGroup, colIndex);
			else if (dataType == SiloDataType.String) array = getStringValues(dataGroup, colIndex);
			else if (dataType == SiloDataType.Integer) array = getIntValues(dataGroup, colIndex);
			else if (dataType == SiloDataType.Long) array = getLongValues(dataGroup, colIndex);
		}

		for (int i=offset; i<getRowCount(dataGroup); i++) {
			T rowObject = getRow(dataGroup, i);
			if (columnDescriptor == null || columnDescriptor.feature == null) {
				if (array instanceof float[]) ((float[])array)[i] = Float.NaN;
				else if (array instanceof double[]) ((double[])array)[i] = Double.NaN;
				else if (array instanceof String[]) ((String[])array)[i] = "";
			} else {
				insertDefaultValue(rowObject, array, i, columnDescriptor);
			}
		}

		return array;
	}

	private void triggerModelEvent(SiloStructure struct, ModelEventType type, boolean persisted) {
		ModelEvent event = new ModelEvent(struct, type, persisted ? 1 : 0);
		ModelEventService.getInstance().fireEvent(event);
	}

	private String getColumn(String dataGroup, int index) throws SiloException {
		return getColumns(dataGroup)[index];
	}
}
