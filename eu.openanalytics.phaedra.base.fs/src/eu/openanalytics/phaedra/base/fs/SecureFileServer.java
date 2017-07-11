package eu.openanalytics.phaedra.base.fs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.List;

import eu.openanalytics.phaedra.base.fs.nio.NIOInterface;
import eu.openanalytics.phaedra.base.fs.preferences.Prefs;
import eu.openanalytics.phaedra.base.fs.smb.SMBInterface;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.RetryingUtils;
import eu.openanalytics.phaedra.base.util.misc.RetryingUtils.RetryingBlock;
import eu.openanalytics.phaedra.base.util.misc.RetryingUtils.RetryingInputAccessor;

/**
 * This class provides secure access to a file server.
 */
public class SecureFileServer {

	private String accountName;
	private String basePath;
	
	private FSInterface fsInterface;
	
	public final static String UNC_PREFIX = "\\\\";
	
	public SecureFileServer(String fsPath, String userName, String password, String wins) {
		this.accountName = userName;
		
		if (fsPath.startsWith(UNC_PREFIX)) this.basePath = UNC_PREFIX + fsPath.substring(UNC_PREFIX.length()).replace('\\', '/');
		else this.basePath = fsPath.replace('\\', '/');
		
		// Use an appropriate FS interface. Prefer NIO over SMB.
		fsInterface = new NIOInterface();
		if (!fsInterface.isCompatible(fsPath, userName)) fsInterface = new SMBInterface();
		if (!fsInterface.isCompatible(fsPath, userName)) throw new RuntimeException("No handler found for file server type " + fsPath);
		
		try {
			fsInterface.initialize(fsPath, userName, password, wins);
		} catch (Exception e) {
			throw new RuntimeException("Failed to connect to file server " + fsPath, e);
		}
	}

	public void close() {
		try {
			fsInterface.close();
		} catch (IOException e) {
			EclipseLog.error("Failed to disconnect file server", e, Activator.getDefault());
		}
	}
	
	private String getFullPath(String path) {
		path = path.replace('\\', '/');
		if (!path.startsWith("/")) path = "/" + path;
		return basePath + path;
	}
	
	/*
	 * Public API ----------
	 */
	
	public String getAccountName() {
		return accountName;
	}
	
	@Deprecated
	public File getAsFile(String path) {
		return fsInterface.getAsFile(getFullPath(path));
	}
	
	public long getFreeSpace() {
		File fsRoot = new File(basePath);
		return fsRoot.getFreeSpace();
	}
	
	public long getTotalSpace() {
		File fsRoot = new File(basePath);
		return fsRoot.getTotalSpace();
	}
	
	public synchronized boolean exists(String path) throws IOException {
		return fsInterface.exists(getFullPath(path));
	}

	public synchronized boolean isDirectory(String path) throws IOException {
		return fsInterface.isDirectory(getFullPath(path));
	}

	public synchronized long getCreateTime(String path) throws IOException {
		return fsInterface.getCreateTime(getFullPath(path));
	}

	public synchronized long getLastModified(String path) throws IOException {
		return fsInterface.getLastModified(getFullPath(path));
	}

	public synchronized List<String> dir(String path) throws IOException {
		return fsInterface.dir(getFullPath(path));
	}

	public synchronized void mkDir(String path) throws IOException {
		fsInterface.mkDir(getFullPath(path));
	}

	public synchronized void mkDirs(String path) throws IOException {
		fsInterface.mkDirs(getFullPath(path));
	}

	public synchronized void delete(String path) throws IOException {
		if (exists(path)) fsInterface.delete(getFullPath(path));
	}

	public synchronized void renameTo(String from, String to) throws IOException {
		fsInterface.renameTo(getFullPath(from), getFullPath(to));
	}

	public synchronized long getLength(String path) throws IOException {
		return fsInterface.getLength(getFullPath(path));
	}

	public SeekableByteChannel getChannel(String path, String mode) throws IOException {
		if (exists(path)) return fsInterface.getChannel(getFullPath(path), mode);
		else return null;
	}
	
	public InputStream getContents(String path) throws IOException {
		return fsInterface.getInputStream(getFullPath(path));
	}

	public synchronized String getContentsAsString(String path) throws IOException {
		InputStream in = getContents(path);
		byte[] contents = StreamUtils.readAll(in);
		return new String(contents);
	}

	public void download(String path, String localDestination) throws IOException {
		File target = new File(localDestination);
		if (!target.exists()) target.mkdirs();
		else if (!target.isDirectory()) throw new IOException("Target is not a directory: " + localDestination);
		
		if (isDirectory(path)) {
			for (String item: dir(path)) {
				download(path + "/" + item, localDestination + "/" + FileUtils.getName(path));
			}
		} else {
			StreamUtils.copyAndClose(getContents(path), new FileOutputStream(localDestination + "/" + FileUtils.getName(path)));
		}
	}
	
	public void copy(String from, String to) throws IOException {
		if (!exists(from)) return;
		putContents(to, getContents(from));
	}
	
	public OutputStream getOutputStream(String path) throws IOException {
		String parentPath = FileUtils.getPath(path);
		if (!fsInterface.exists(parentPath)) fsInterface.mkDirs(parentPath);
		return fsInterface.getOutputStream(getFullPath(path));
	}

	public synchronized void putContents(final String path, final byte[] contents) throws IOException {
		RetryingInputAccessor accessor = new RetryingInputAccessor() {
			@Override
			public InputStream getInput() throws IOException {
				return new ByteArrayInputStream(contents);
			}
		};
		putContents(path, accessor);
	}

	public synchronized void putContents(final String path, final File contents) throws IOException {
		RetryingInputAccessor accessor = new RetryingInputAccessor() {
			@Override
			public InputStream getInput() throws IOException {
				return new FileInputStream(contents);
			}
		};
		putContents(path, accessor);
	}
	
	public synchronized void putContents(final String path, final RetryingInputAccessor accessor) throws IOException {
		RetryingBlock uploader = new RetryingBlock() {
			@Override
			public void run() throws Exception {
				InputStream in = null;
				try {
					in = accessor.getInput();
					putContents(path, in);
				} finally {
					if (in != null) try { in.close(); } catch (IOException e) {}
				}
			}
		};
		
		int tries = Activator.getDefault().getPreferenceStore().getInt(Prefs.UPLOAD_RETRIES);
		int delay = Activator.getDefault().getPreferenceStore().getInt(Prefs.UPLOAD_RETRY_DELAY);
		
		try {
			RetryingUtils.doRetrying(uploader, tries, delay);
		} catch (Exception e) {
			throw new IOException("Upload failed: " + path, e);
		}
	}
	
	public synchronized void putContents(String path, InputStream input) throws IOException {
		String fullPath = getFullPath(path);
		String parentPath = FileUtils.getPath(fullPath);
		if (!fsInterface.exists(parentPath)) fsInterface.mkDirs(parentPath);
		OutputStream out = fsInterface.getOutputStream(fullPath);
		StreamUtils.copyAndClose(input, out);
	}
	
	public synchronized void renameAndReplace(String from, String to) throws IOException {
		int step = 1;
		boolean toExists = false;
		try {
			toExists = exists(to);
			if (toExists) renameTo(to, to + ".backup");
			else {
				String parentPath = FileUtils.getPath(to) + "/";
				if (!exists(parentPath)) mkDirs(parentPath);		
			}
			step++;
			renameTo(from, to);
			step++;
			if (toExists) delete(to + ".backup");
		} catch (IOException e) {
			if (step == 1) {
				// Failed to create backup. Fail altogether, nothing has changed.
				throw new IOException("Safe rename failed for " + to, e);
			} else if (step == 2) {
				// Backup created but rename failed. Restore backup.
				if (toExists) {
					try { renameTo(to + ".backup", to); } catch (IOException e1) {}
				}
				throw new IOException("Safe rename failed for " + to, e);
			} else if (step == 3) {
				// Failed to delete backup. This is non-critical, treat rename as succesfull.
			}
		}
	}
	
	public synchronized void safeReplace(String fsPath, File newFile) throws IOException {
		try {
			// Try the rename-based method first.
			safeReplaceMove(fsPath, newFile);
			return;
		} catch (IOException e) {
			// Fall back to the copy-based method below.
		}
		
		// Safe upload: copy original to ".backup", upload new, delete backup.
		String backupPath = fsPath + ".backup";
		int step = 0;
		putContents(backupPath, getContents(fsPath));
		try {
			step = 1;
			putContents(fsPath, newFile);
			step = 2;
			delete(backupPath);
		} catch (Throwable t) {
			// Restore backup file.
			if (step == 1) {
				// The upload failed, delete the incomplete upload and restore the backup file
				try { putContents(fsPath, getContents(backupPath)); } catch (Throwable t2) {}
				delete(backupPath);
			} else if (step == 2) {
				// The backup delete failed. Keep the newly uploaded file and consider the replace successful.
				return;
			}
			throw t;
		}
	}
	
	private synchronized void safeReplaceMove(String fsPath, File newFile) throws IOException {
		// Faster, but requires delete access to the file (because of rename).
		
		// Safe upload: rename original to ".old", upload new, delete old.
		int step = 0;
		renameTo(fsPath, fsPath + ".old");
		try {
			step = 1;
			putContents(fsPath, newFile);
			step = 2;
			delete(fsPath + ".old");
		} catch (Throwable t) {
			// Restore original file.
			if (step == 1) {
				// The upload failed, delete the incomplete upload and restore the original .old file
				try { delete(fsPath); } catch (Throwable t2) {}
				renameTo(fsPath + ".old", fsPath);
			} else if (step == 2) {
				// The .old delete failed. Keep the newly uploaded file and consider the replace successful.
				return;
			}
			throw t;
		}
	}
}
