package eu.openanalytics.phaedra.base.ui.admin.fs;

import java.io.File;
import java.io.IOException;

import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.security.SecurityService;

public class AdminFileServer {

	private static AdminFileServer instance;
	
	private AdminFileServer() {
		// Hidden constructor.
	}
	
	public static AdminFileServer getInstance() {
		if (instance == null) instance = new AdminFileServer();
		return instance;
	}
	
	public void saveFile(File file, byte[] contents) throws IOException {
		if (!SecurityService.getInstance().isGlobalAdmin()) return;
		
		String absolutePath = file.getAbsolutePath();
		String fsRoot = Screening.getEnvironment().getFileServer().getBasePath();
		String fsPath = absolutePath.substring(fsRoot.length());
		Screening.getEnvironment().getFileServer().putContents(fsPath, contents);
	}
	
	public void deleteFile(File file) throws IOException {
		if (!SecurityService.getInstance().isGlobalAdmin()) return;
		
		String absolutePath = file.getAbsolutePath();
		String fsRoot = Screening.getEnvironment().getFileServer().getBasePath();
		String fsPath = absolutePath.substring(fsRoot.length());
		if (file.isDirectory()) fsPath += "/";
		Screening.getEnvironment().getFileServer().delete(fsPath);
	}
	
	public void createDir(File parent, String name) throws IOException {
		if (!SecurityService.getInstance().isGlobalAdmin()) return;
		
		String absolutePath = parent.getAbsolutePath();
		String fsRoot = Screening.getEnvironment().getFileServer().getBasePath();
		String fsPath = absolutePath.substring(fsRoot.length()) + "/" + name + "/";
		Screening.getEnvironment().getFileServer().mkDir(fsPath);
	}
	
	public void createFile(File parent, String name) throws IOException {
		if (!SecurityService.getInstance().isGlobalAdmin()) return;
		
		String absolutePath = parent.getAbsolutePath();
		String fsRoot = Screening.getEnvironment().getFileServer().getBasePath();
		String fsPath = absolutePath.substring(fsRoot.length()) + "/" + name;
		Screening.getEnvironment().getFileServer().putContents(fsPath, new byte[0]);
	}
}
