package eu.openanalytics.phaedra.base.fs.nio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.openanalytics.phaedra.base.fs.BaseFileServer;
import eu.openanalytics.phaedra.base.fs.SMBHelper;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;

public class NIOInterface extends BaseFileServer {
	
	private String basePath;
	
	@Override
	public boolean isCompatible(String fsPath, String userName) {
		if (ProcessUtils.isWindows() && SMBHelper.isSMBPath(fsPath)) {
			String fsUser = userName.substring(userName.indexOf("\\") + 1);
			String currentUser = System.getProperty("user.name");
			return fsUser.equalsIgnoreCase(currentUser);
		} else if (!SMBHelper.isSMBPath(fsPath)) {
			Path path = null;
			try {
				path = Paths.get(fsPath);
				Files.createDirectories(path);
			} catch (Exception e) {
				return false;
			}
			return path != null && Files.exists(path);
		}
		return false;
	}
	
	@Override
	public void initialize(String fsPath, String userName, String pw) throws IOException {
		if (fsPath.startsWith(SMBHelper.SMB_PROTOCOL_PREFIX)) fsPath = SMBHelper.toUNCNotation(fsPath);
		basePath = fsPath;
	}
	
	@Override
	public void close() throws IOException {
		// Nothing to do.
	}
	
	@Override
	public long getFreeSpace() throws IOException {
		return getNIOPath("/").toFile().getFreeSpace();
	}
	
	@Override
	public long getTotalSpace() throws IOException {
		return getNIOPath("/").toFile().getTotalSpace();
	}
	
	@Override
	public boolean exists(String path) throws IOException {
		return Files.exists(getNIOPath(path));
	}
	
	@Override
	public boolean isDirectory(String path) throws IOException {
		return Files.isDirectory(getNIOPath(path));
	}
	
	@Override
	public long getCreateTime(String path) throws IOException {
		return Files.readAttributes(getNIOPath(path), BasicFileAttributes.class).creationTime().toMillis();
	}
	
	@Override
	public long getLastModified(String path) throws IOException {
		return Files.getLastModifiedTime(getNIOPath(path)).toMillis();
	}
	
	@Override
	public List<String> dir(String path) throws IOException {
		List<String> children = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(getNIOPath(path))) {
		    for (Path file: stream) {
		    	children.add(file.getFileName().toString());
		    }
		}
		return children;
	}
	
	@Override
	public void mkDir(String path) throws IOException {
		Files.createDirectory(getNIOPath(path));
	}
	
	@Override
	public void mkDirs(String path) throws IOException {
		Files.createDirectories(getNIOPath(path));
	}

	@Override
	public void delete(String path) throws IOException {
		Path p = getNIOPath(path);
		if (Files.isDirectory(p)) {
			Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});			
		} else {
			if (Files.exists(p)) Files.delete(p);
		}
	}
	
	@Override
	public long getLength(String path) throws IOException {
		return Files.size(getNIOPath(path));
	}
	
	@Override
	public InputStream getInputStream(String path) throws IOException {
		return new FileInputStream(getNIOPath(path).toFile());
	}

	@Override
	public SeekableByteChannel getChannel(String path, String mode) throws IOException {
		Set<OpenOption> opts = new HashSet<>();
		if (mode.toLowerCase().contains("r")) opts.add(StandardOpenOption.READ);
		if (mode.toLowerCase().contains("w")) opts.add(StandardOpenOption.WRITE);
		return Files.newByteChannel(getNIOPath(path), opts);
	}
	
	@Override
	protected void doRenameTo(String from, String to) throws IOException {
		Files.move(getNIOPath(from), getNIOPath(to));
	}
	
	@Override
	protected void doUpload(String path, InputStream input, long length) throws IOException {
		StreamUtils.copyAndClose(input, new FileOutputStream(getNIOPath(path).toFile()));
	}
	
	@Override
	public File getAsFile(String path) {
		return getNIOPath(path).toFile();
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private Path getNIOPath(String path) {
		if (path.startsWith("/") || path.startsWith("\\")) path = path.substring(1);
		return Paths.get(basePath, path);
	}
}
