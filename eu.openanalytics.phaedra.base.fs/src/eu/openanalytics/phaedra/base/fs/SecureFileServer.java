package eu.openanalytics.phaedra.base.fs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.ExtensionUtils;

/**
 * This class provides secure access to a file server.
 */
public class SecureFileServer {

	private FSInterface fsInterface;
	
	private final static String EXT_PT_ID = Activator.PLUGIN_ID + ".fileServerType";
	private final static String ATTR_CLASS = "class";
	private final static String ATTR_PRIORITY = "priority";
	
	public SecureFileServer(FileServerConfig cfg) {
		String fsPath = cfg.get(FileServerConfig.PATH);
		
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_PT_ID);
		Function<IConfigurationElement, Integer> priority = e -> {
			String prio = e.getAttribute(ATTR_PRIORITY);
			return (prio == null) ? 1 : Integer.parseInt(prio);
		};
		fsInterface = Arrays.stream(elements)
				.sorted((e1, e2) -> priority.apply(e2) - priority.apply(e1))
				.map(e -> ExtensionUtils.createInstance(e, ATTR_CLASS, FSInterface.class))
				.filter(fs -> fs.isCompatible(cfg))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("No handler found for file server type " + fsPath));
		
		try {
			fsInterface.initialize(cfg);
			EclipseLog.info("Using " + fsInterface.getClass().getName(), Activator.getDefault());
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
	
	/*
	 * Public API ----------
	 */
	
	@Deprecated
	public File getAsFile(String path) {
		return fsInterface.getAsFile(path);
	}
	
	public synchronized long getFreeSpace() throws IOException {
		return fsInterface.getFreeSpace();
	}
	
	public synchronized long getTotalSpace() throws IOException {
		return fsInterface.getTotalSpace();
	}
	
	public synchronized boolean exists(String path) throws IOException {
		return fsInterface.exists(path);
	}

	public synchronized boolean isDirectory(String path) throws IOException {
		return fsInterface.isDirectory(path);
	}

	public synchronized long getCreateTime(String path) throws IOException {
		return fsInterface.getCreateTime(path);
	}

	public synchronized long getLastModified(String path) throws IOException {
		return fsInterface.getLastModified(path);
	}

	public synchronized List<String> dir(String path) throws IOException {
		return fsInterface.dir(path);
	}

	public synchronized void mkDir(String path) throws IOException {
		fsInterface.mkDir(path);
	}

	public synchronized void mkDirs(String path) throws IOException {
		fsInterface.mkDirs(path);
	}

	public synchronized void delete(String path) throws IOException {
		fsInterface.delete(path);
	}

	public synchronized void renameTo(String from, String to) throws IOException {
		fsInterface.renameTo(from, to);
	}

	public synchronized long getLength(String path) throws IOException {
		return fsInterface.getLength(path);
	}

	public SeekableByteChannel getChannel(String path, String mode) throws IOException {
		if (exists(path)) return fsInterface.getChannel(path, mode);
		else return null;
	}
	
	public InputStream getContents(String path) throws IOException {
		return fsInterface.getInputStream(path);
	}

	public synchronized void putContents(final String path, final byte[] contents) throws IOException {
		fsInterface.upload(path, contents);
	}

	public synchronized void putContents(final String path, final File contents) throws IOException {
		fsInterface.upload(path, contents);
	}
	
	public synchronized void putContents(String path, InputStream input) throws IOException {
		fsInterface.upload(path, input);
	}
	
	public synchronized void renameAndReplace(String from, String to) throws IOException {
		fsInterface.renameTo(from, to);
	}
	
	public synchronized void safeReplace(String path, File newFile) throws IOException {
		fsInterface.upload(path, newFile);
	}
	
	public synchronized String getContentsAsString(String path) throws IOException {
		return fsInterface.downloadAsString(path);
	}

	public synchronized void download(String path, String localDestination) throws IOException {
		fsInterface.download(path, localDestination);
	}
	
	public synchronized void copy(String from, String to) throws IOException {
		fsInterface.copy(from, to);
	}
}
