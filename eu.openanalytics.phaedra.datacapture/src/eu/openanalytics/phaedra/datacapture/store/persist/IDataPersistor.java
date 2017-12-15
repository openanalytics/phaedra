package eu.openanalytics.phaedra.datacapture.store.persist;

import java.io.IOException;

import eu.openanalytics.phaedra.base.fs.store.IFileStore;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public interface IDataPersistor {

	public void persist(IFileStore store, Plate plate) throws DataCaptureException, IOException;

}
