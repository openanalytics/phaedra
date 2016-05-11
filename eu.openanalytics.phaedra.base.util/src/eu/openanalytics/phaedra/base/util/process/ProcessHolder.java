package eu.openanalytics.phaedra.base.util.process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;

public class ProcessHolder {

	private String tempDir;
	
	public void initialize(String bundleId, String[] filePaths) throws IOException {
		tempDir = FileUtils.generateTempFolder(true);
		
		Bundle bundle = Platform.getBundle(bundleId);
		
		for (String file: filePaths) {
			IPath path = new Path(file);
			URL url = FileLocator.find(bundle, path, null);
			InputStream in = url.openStream();
			String destination = tempDir + "/" + FileUtils.getName(file);
			OutputStream out = new FileOutputStream(destination);
			StreamUtils.copyAndClose(in, out);
		}
	}
	
	public String getDir() {
		return tempDir;
	}
	
	public void dispose() {
		FileUtils.deleteRecursive(new File(tempDir));
	}
}
