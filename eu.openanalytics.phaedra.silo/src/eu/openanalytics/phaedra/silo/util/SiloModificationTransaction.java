package eu.openanalytics.phaedra.silo.util;

import java.io.IOException;

import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.hdf5.util.EntityModificationTransaction;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.SecurityService.Action;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.silo.SiloDataService;
import eu.openanalytics.phaedra.silo.vo.Silo;

/**
 * <p>This helper class enables multiple write operations on a subwell data file (HDF5) within
 * a transaction.</p>
 * <p>Start the transaction with begin(), then perform one or more operations on the file using
 * getWorkingCopy(). Finally, finish the transaction using commit().</p>
 * <p>If something goes wrong, use rollback(). Only when commit() is successfully called will
 * the changes be made to the server version of the data file.</p>
 */
public class SiloModificationTransaction extends EntityModificationTransaction {

	private Silo silo;
	
	public SiloModificationTransaction(Silo silo) {
		// Security check.
		SecurityService.getInstance().checkPersonalObjectWithException(Action.READ, silo);
		this.silo = silo;
	}

	@Override
	public void begin() throws IOException {
		// Instead of downloading the existing HDF5 file, create a new one.
		// All datasets will be overwritten anyway (see SiloDataService).
		String tempFolder = FileUtils.generateTempFolder(true);
		String localHDF5Path = tempFolder + "/" + getId() + ".h5";
		setWorkingCopy(new HDF5File(localHDF5Path, false));
	}
	
	@Override
	public void commit() throws IOException {
		SecurityService.getInstance().checkPersonalObjectWithException(Action.UPDATE, silo);
		super.commit();
	}
	
	public Silo getSilo() {
		return silo;
	}

	@Override
	protected long getId() {
		return silo.getId();
	}

	@Override
	protected String getHDF5PathFull() {
		return SiloDataService.getInstance().getSiloFSPath(silo, true);
	}

	@Override
	protected String getHDF5PathRelative() {
		return SiloDataService.getInstance().getSiloFSPath(silo, false);
	}

}