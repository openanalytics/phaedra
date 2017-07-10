package eu.openanalytics.phaedra.base.hdf5.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.hdf5.HDF5File;
import eu.openanalytics.phaedra.base.hdf5.HDF5FileRegistry;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

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
		String remotePath = getHDF5Path();
		String localPath = FileUtils.generateTempFolder(true) + "/" + getId() + ".h5";
		
		if (Screening.getEnvironment().getFileServer().exists(remotePath)) {
			StreamUtils.copyAndClose(Screening.getEnvironment().getFileServer().getContents(remotePath), new FileOutputStream(localPath));
		} else {
			// Create a blank working copy.
			HDF5File hdf5File = new HDF5File(localPath, false);
			hdf5File.open();
			hdf5File.close();
		}
		
		workingCopy = new HDF5File(localPath, false);
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
		String remotePath = getHDF5Path();
		
		boolean fileExists = Screening.getEnvironment().getFileServer().exists(remotePath);
		if (fileExists) {
			// Re-upload the modified HDF5 file to the file server.
			// Because HDF5 is not threadsafe, all open handles to this HDF5 file must be closed during the upload!
			HDF5FileRegistry.closeAll(remotePath);
			Screening.getEnvironment().getFileServer().safeReplace(remotePath, new File(workingCopy.getFilePath()));
			HDF5FileRegistry.reopenAll(remotePath);
		} else {
			Screening.getEnvironment().getFileServer().putContents(remotePath, new File(workingCopy.getFilePath()));
			// Because SubwellService may not see the HDF5 file appear immediately, wait a bit to prevent issues.
			int maxTries = 20;
			int currentTry = 1;
			while (currentTry++ < maxTries && !(new File(getHDF5Path()).exists())) {
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
	
	protected abstract String getHDF5Path();

}