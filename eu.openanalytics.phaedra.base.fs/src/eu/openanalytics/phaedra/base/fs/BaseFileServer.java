package eu.openanalytics.phaedra.base.fs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import eu.openanalytics.phaedra.base.fs.preferences.Prefs;
import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.RetryingUtils;
import eu.openanalytics.phaedra.base.util.misc.RetryingUtils.RetryingInputAccessor;

public abstract class BaseFileServer implements FSInterface {

	@Override
	public String downloadAsString(String path) throws IOException {
		InputStream in = getInputStream(path);
		byte[] contents = StreamUtils.readAll(in);
		return new String(contents);
	}

	@Override
	public void download(String path, String localDestination) throws IOException {
		File target = new File(localDestination);
		if (!target.exists()) target.mkdirs();
		else if (!target.isDirectory()) throw new IOException("Target is not a directory: " + localDestination);
		
		if (isDirectory(path)) {
			for (String item: dir(path)) {
				download(path + "/" + item, localDestination + "/" + FileUtils.getName(path));
			}
		} else {
			StreamUtils.copyAndClose(getInputStream(path), new FileOutputStream(localDestination + "/" + FileUtils.getName(path)));
		}
	}
	
	@Override
	public void copy(String from, String to) throws IOException {
		if (!exists(from)) return;
		upload(to, getInputStream(from));
	}
	
	@Override
	public void renameTo(String oldPath, String newPath) throws IOException {
		safeModification(newPath, () -> {
			doRenameTo(oldPath, newPath);
		});
	}

	protected abstract void doRenameTo(String from, String to) throws IOException;
	
	@Override
	public void upload(String path, File file) throws IOException {
		uploadRetrying(path, new RetryingInputAccessor() {
			@Override
			public InputStream getInput() throws IOException {
				return new FileInputStream(file);
			}
			@Override
			public long getInputLength() {
				return file.length();
			}
		});
	}
	
	@Override
	public void upload(String path, byte[] bytes) throws IOException {
		uploadRetrying(path, new RetryingInputAccessor() {
			@Override
			public InputStream getInput() throws IOException {
				return new ByteArrayInputStream(bytes);
			}
			@Override
			public long getInputLength() {
				return bytes.length;
			}
		});
	}
	
	@Override
	public void upload(String path, InputStream input) throws IOException {
		// No retrying possible here: inputstreams cannot be reset.
		safeModification(path, () -> doUpload(path, input, -1));
	}
	
	protected abstract void doUpload(String path, InputStream input, long length) throws IOException;
	
	protected void uploadRetrying(String path, RetryingInputAccessor accessor) throws IOException {
		int tries = Activator.getDefault().getPreferenceStore().getInt(Prefs.UPLOAD_RETRIES);
		int delay = Activator.getDefault().getPreferenceStore().getInt(Prefs.UPLOAD_RETRY_DELAY);
		try {
			RetryingUtils.doRetrying(() -> {
				safeModification(path, () -> {
					try (InputStream in = accessor.getInput()) {
						doUpload(path, in, accessor.getInputLength());
					}
				});
			}, tries, delay);
		} catch (Exception e) {
			throw new IOException("Upload failed: " + path, e);
		}
	}
	
	protected void safeModification(String target, IORunnable runnable) throws IOException {
		int stage = 1;
		boolean destinationExists = false;
		try {
			destinationExists = exists(target);
			if (destinationExists) {
				try {
					doRenameTo(target, target + ".backup");
				} catch (Exception e) {
					upload(target + ".backup", getInputStream(target));
				}
			} else {
				String parentPath = FileUtils.getPath(target) + "/";
				if (!exists(parentPath)) mkDirs(parentPath);
			}
			stage = 2;
			runnable.run();
			stage = 3;
			if (destinationExists) delete(target + ".backup");
		} catch (IOException e) {
			if (stage == 1) {
				// Failed to create backup. Fail altogether, nothing has changed.
				throw new IOException("Could not replace " + target, e);
			} else if (stage == 2) {
				// Backup created but rename failed. Restore the backup.
				if (destinationExists) {
					try { doRenameTo(target + ".backup", target); } catch (IOException e1) {}
				}
				throw new IOException("Could not replace " + target, e);
			} else if (stage == 3) {
				// Failed to delete the backup. This is non-critical, treat rename as succesfull.
			}
		}
	}
	
	protected static interface IORunnable {
		public void run() throws IOException;
	}
}