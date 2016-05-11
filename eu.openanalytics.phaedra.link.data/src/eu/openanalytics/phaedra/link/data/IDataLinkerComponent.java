package eu.openanalytics.phaedra.link.data;

import org.eclipse.core.runtime.IProgressMonitor;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.datacapture.model.PlateReading;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public interface IDataLinkerComponent {

	public void prepareLink(PlateReading reading, HDF5File dataFile, Plate destination, IProgressMonitor monitor) throws DataLinkException;		

	public void executeLink() throws DataLinkException;

	public void rollback();
}
