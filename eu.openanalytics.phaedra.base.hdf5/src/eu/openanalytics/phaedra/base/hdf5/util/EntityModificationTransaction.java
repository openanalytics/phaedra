package eu.openanalytics.phaedra.base.hdf5.util;

import java.io.File;
import java.io.IOException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.hdf5.HDF5FileRegistry;
import eu.openanalytics.phaedra.base.util.io.FileUtils;

/**
 * <p>This helper class enables multiple write operations on a subwell data file (HDF5) within
 * a transaction.</p>
 * <p>Start the transaction with begin(), then perform one or more operations on the file using
 * getWorkingCopy(). Finally, finish the transaction using commit().</p>
 * <p>If something goes wrong, use rollback(). Only when commit() is successfully called will
 * the changes be made to the server version of the data file.</p>
 */
public abstract class EntityModificationTransaction {

	private HDF5File workingCopy;

	public void begin() throws IOException {
		String hdf5PathFull = getHDF5PathFull();
		String tempFolder = FileUtils.generateTempFolder(true);
		String localHDF5Path = tempFolder + "/" + getId() + ".h5";
		
		if (new File(hdf5PathFull).exists()) {
			// Download the file into a working copy.
			FileUtils.copy(hdf5PathFull, localHDF5Path);
		} else {
			// Create a blank working copy.
			HDF5File hdf5File = new HDF5File(localHDF5Path, false);
			hdf5File.open();
			hdf5File.close();
		}
		
		workingCopy = new HDF5File(localHDF5Path, false);
	}

	/**
	 * If a working copy is already present, do not open a new one.
	 * @throws IOException
	 */
	public void smartBegin() throws IOException {
		if (workingCopy != null) return;
		begin();
	}
	
	public HDF5File getWorkingCopy() {
		return workingCopy;
	}

	public void commit() throws IOException {
		if (workingCopy == null) throw new IOException("No transaction active");
		workingCopy.close();
		String hdf5PathRelative = getHDF5PathRelative();
		
		boolean fileExists = Screening.getEnvironment().getFileServer().exists(hdf5PathRelative);
		if (fileExists) {
			// Re-upload the modified HDF5 file to the file server.
			// Because HDF5 is not threadsafe, all open handles to this HDF5 file must be closed during the upload!
			HDF5FileRegistry.closeAll(hdf5PathRelative);
			Screening.getEnvironment().getFileServer().safeReplace(hdf5PathRelative, new File(workingCopy.getFilePath()));
			HDF5FileRegistry.reopenAll(hdf5PathRelative);
		} else {
			Screening.getEnvironment().getFileServer().putContents(hdf5PathRelative, new File(workingCopy.getFilePath()));
			// Because SubwellService may not see the HDF5 file appear immediately, wait a bit to prevent issues.
			int maxTries = 20;
			int currentTry = 1;
			while (currentTry++ < maxTries && !(new File(getHDF5PathFull()).exists())) {
				try { Thread.sleep(1000); } catch (InterruptedException e) {}
			}
		}
		workingCopy = null;
	}


	public void rollback() {
		if (workingCopy == null) return; // Nothing to rollback.
		workingCopy.close();
		// Delete the temp folder containing the working copy.
		FileUtils.deleteRecursive(new File(workingCopy.getFilePath()).getParentFile());
		workingCopy = null;
	}
	
	protected void setWorkingCopy(HDF5File workingCopy) {
		this.workingCopy = workingCopy;
	}

	protected abstract long getId();
	
	protected abstract String getHDF5PathFull();
	
	protected abstract String getHDF5PathRelative();

}