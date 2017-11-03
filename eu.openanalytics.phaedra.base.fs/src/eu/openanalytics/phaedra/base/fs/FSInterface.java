package eu.openanalytics.phaedra.base.fs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.List;

public interface FSInterface {

	public boolean isCompatible(String fsPath, String userName);
	public void initialize(String fsPath, String userName, String pw) throws IOException;

	public long getFreeSpace() throws IOException;
	public long getTotalSpace() throws IOException;
	
	public void close() throws IOException;
	
	public boolean exists(String path) throws IOException;
	
	public boolean isDirectory(String path) throws IOException;
	
	public long getCreateTime(String path) throws IOException;
	
	public long getLastModified(String path) throws IOException;
	
	public List<String> dir(String path) throws IOException;
	
	public void mkDir(String path) throws IOException;
	
	public void mkDirs(String path) throws IOException;

	public void delete(String path) throws IOException;
	
	public void renameTo(String oldPath, String newPath) throws IOException;
	
	public long getLength(String path) throws IOException;
	
	public InputStream getInputStream(String path) throws IOException;

	public SeekableByteChannel getChannel(String path, String mode) throws IOException;
	
	public void upload(String path, File file) throws IOException;
	public void upload(String path, byte[] bytes) throws IOException;
	public void upload(String path, InputStream input) throws IOException;

	public String downloadAsString(String path) throws IOException;
	public void download(String path, String localDestination) throws IOException;
	public void copy(String from, String to) throws IOException;
	
	@Deprecated
	public File getAsFile(String path);
}
