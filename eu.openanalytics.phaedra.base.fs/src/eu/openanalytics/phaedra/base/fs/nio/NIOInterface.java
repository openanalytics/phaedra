package eu.openanalytics.phaedra.base.fs.nio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import eu.openanalytics.phaedra.base.fs.FSInterface;
import eu.openanalytics.phaedra.base.fs.SecureFileServer;

public class NIOInterface implements FSInterface {
	
	@Override
	public boolean isCompatible(String fsPath, String userName) {
		// Works with local paths, or UNC paths if they require no alternate credentials.
		if (userName.contains("\\")) userName = userName.substring(userName.indexOf("\\") + 1);
		if (userName.equalsIgnoreCase(System.getProperty("user.name"))) return true;
		return (!fsPath.startsWith(SecureFileServer.UNC_PREFIX));
	}
	
	@Override
	public void initialize(String fsPath, String userName, String pw, String wins) throws IOException {
		// Nothing to do.
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
			Files.delete(p);
		}
	}
	
	@Override
	public void renameTo(String oldPath, String newPath) throws IOException {
		Files.move(getNIOPath(oldPath), getNIOPath(newPath));
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
	public OutputStream getOutputStream(String path) throws IOException {
		return new FileOutputStream(getNIOPath(path).toFile());
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private Path getNIOPath(String path) {
		return FileSystems.getDefault().getPath(path);
	}
}
