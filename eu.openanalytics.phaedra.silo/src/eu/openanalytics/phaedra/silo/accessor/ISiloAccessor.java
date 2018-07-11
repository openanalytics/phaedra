package eu.openanalytics.phaedra.silo.accessor;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloDataset;
import eu.openanalytics.phaedra.silo.vo.SiloDatasetColumn;

public interface ISiloAccessor<T> {

	public Silo getSilo();
	public boolean isDirty();
	
	public SiloDataset createDataset(String datasetName) throws SiloException;
	public void removeDataset(String datasetName) throws SiloException;
	
	public SiloDatasetColumn createColumn(String datasetName, String columnName, SiloDataType dataType) throws SiloException;
	public void removeColumn(String datasetName, String columnName) throws SiloException;
	public SiloDataType getColumnDataType(String datasetName, String columnName) throws SiloException;
	
	public int getRowCount(String datasetName) throws SiloException;
	public T getRowObject(String datasetName, int rowIndex) throws SiloException;
	public int getIndexOfRow(String datasetName, T rowObject) throws SiloException;
	public Iterator<T> getRowIterator(String datasetName) throws SiloException;

	public void addRows(String datasetName, T[] rows) throws SiloException;
	public void removeRows(String datasetName, int[] rows) throws SiloException;

	public float[] getFloatValues(String datasetName, String columnName) throws SiloException;
	public long[] getLongValues(String datasetName, String columnName) throws SiloException;
	public String[] getStringValues(String datasetName, String columnName) throws SiloException;

	/**
	 * Update the contents of a dataset column.
	 * The data must be an array, with its size matching the row count of the dataset.
	 * 
	 * @param datasetName The name of the dataset to update a column in.
	 * @param columnName The name of the column to update.
	 * @param newData The data array to place in the column.
	 * @throws SiloException If the column cannot be updated for any reason.
	 */
	public void updateValues(String datasetName, String columnName, Object newData) throws SiloException;

	/**
	 * Save any changes made into the silo.
	 * 
	 * @param monitor An optional progress monitor.
	 * @throws SiloException If the changes cannot be saved for any reason.
	 */
	public void save(IProgressMonitor monitor) throws SiloException;
	
	/**
	 * Revert any changes made, so that this accessor reflects the current saved state of the silo.
	 */
	public void revert();

}