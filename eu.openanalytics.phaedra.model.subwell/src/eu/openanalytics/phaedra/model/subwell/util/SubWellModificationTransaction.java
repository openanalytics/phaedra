package eu.openanalytics.phaedra.model.subwell.util;

import eu.openanalytics.phaedra.base.hdf5.util.EntityModificationTransaction;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

/**
 * <p>This helper class enables multiple write operations on a subwell data file (HDF5) within
 * a transaction.</p>
 * <p>Start the transaction with begin(), then perform one or more operations on the file using
 * getWorkingCopy(). Finally, finish the transaction using commit().</p>
 * <p>If something goes wrong, use rollback(). Only when commit() is successfully called will
 * the changes be made to the server version of the data file.</p>
 */
public class SubWellModificationTransaction extends EntityModificationTransaction {

	private Plate plate;
	
	public SubWellModificationTransaction(Plate plate) {
		// Security check.
		SecurityService.getInstance().checkWithException(Permissions.PLATE_EDIT, plate);
		this.plate = plate;
	}
	
	public Plate getPlate() {
		return plate;
	}

	@Override
	protected long getId() {
		return plate.getId();
	}
	
	@Override
	protected String getHDF5Path() {
		return PlateService.getInstance().getPlateFSPath(plate) + "/" + plate.getId() + ".h5";
	}
}
