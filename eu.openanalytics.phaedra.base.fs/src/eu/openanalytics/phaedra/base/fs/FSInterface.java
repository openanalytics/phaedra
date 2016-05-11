package eu.openanalytics.phaedra.base.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FSInterface {

	public boolean isCompatible(String fsPath, String userName);
	
	public void initialize(String fsPath, String userName, String pw, String wins) throws IOException;

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

	public OutputStream getOutputStream(String path) throws IOException;
}
