package eu.openanalytics.phaedra.silo.accessor;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.util.SiloStructure;
import eu.openanalytics.phaedra.silo.vo.Silo;

public interface ISiloAccessor<T> {

	public Silo getSilo();

	public SiloStructure getSiloStructure() throws SiloException;

	public String[] getDataGroups() throws SiloException;
	public String[] getColumns(String dataGroup) throws SiloException;

	public int getRowCount(String dataGroup) throws SiloException;
	public T getRow(String dataGroup, int rowIndex) throws SiloException;
	public int getRow(String dataGroup, T object) throws SiloException;
	public Iterator<T> getRowIterator(String dataGroup) throws SiloException;

	public SiloDataType getDataType(String dataGroup, int column) throws SiloException;

	public float[] getFloatValues(String dataGroup, int column) throws SiloException;
	public String[] getStringValues(String dataGroup, int column) throws SiloException;
	public int[] getIntValues(String dataGroup, int column) throws SiloException;
	public long[] getLongValues(String dataGroup, int column) throws SiloException;
	public double[] getDoubleValues(String dataGroup, int column) throws SiloException;

	public void addRows(String dataGroup, T[] rows, SiloStructure siloStructure) throws SiloException;
	public void addRows(String dataGroup, T[] rows) throws SiloException;
	public void removeRows(String dataGroup, int[] rows) throws SiloException;

	public void addColumn(String dataGroup, String columnName) throws SiloException;
	public void replaceColumn(String dataGroup, String columnName, Object data) throws SiloException;

	public boolean isEditable(String columnName);
	public String[] getMandatoryColumns();

	public void save(IProgressMonitor monitor) throws SiloException;
	public void revert() throws SiloException;

}