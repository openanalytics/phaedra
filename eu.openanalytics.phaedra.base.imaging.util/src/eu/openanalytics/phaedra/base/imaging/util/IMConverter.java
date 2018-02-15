package eu.openanalytics.phaedra.base.imaging.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

import eu.openanalytics.phaedra.base.util.io.FileUtils;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;

public class IMConverter {

	private static String executable;
	
	private static void initialize() throws IOException {
		if (ProcessUtils.isWindows()) {
			String tempDir = FileUtils.generateTempFolder(true);
			String[] requiredFiles = { "os/win32/x86_64/convert.exe", "os/win32/x86_64/vcomp100.dll" };
			copyFiles(requiredFiles, tempDir);
			executable = tempDir + "/convert.exe";
		} else if (ProcessUtils.isMac()) {
			String tempDir = FileUtils.generateTempFolder(true);
			String[] requiredFiles = { "os/macosx/x86_64/im.tar.gz" };
			copyFiles(requiredFiles, tempDir);
			executable = tempDir + "/bin/convert";
			try {
				ProcessUtils.execute(new String[] { "tar", "-xzf", "im.tar.gz" }, tempDir);
			} catch (InterruptedException e) {}
		} else {
			// Linux: assume ImageMagick is pre-installed
			executable = "convert";
		}
	}
	
	private static void copyFiles(String[] files, String destination) throws IOException {
		Bundle bundle = Activator.getContext().getBundle();
		for (String file: files) {
			URL url = FileLocator.find(bundle, new Path(file), null);
			String fullDestination = destination + "/" + FileUtils.getName(file);
			StreamUtils.copyAndClose(url.openStream(), new FileOutputStream(fullDestination));
		}
	}
	
	public static void convert(String input, String args, String output) throws IOException {
		String[] argArray = null;
		if (args.contains(" ")) {
			String[] parts = args.split(" ");
			argArray = new String[2+parts.length];
			argArray[0] = input;
			argArray[argArray.length-1] = output;
			for (int i=0; i<parts.length; i++) argArray[1+i] = parts[i];
		} else {
			argArray = new String[]{input,args,output};
		}
		convert(argArray);
	}
	
	public static void convert(String[] args) throws IOException {
		synchronized(IMConverter.class) {
			if (executable == null) initialize();
		}
		
		String[] cmd = new String[args.length + 1];
		cmd[0] = executable;
		for (int i=0; i<args.length; i++) cmd[1+i] = args[i];
		
		boolean ignoreExitCode = Boolean.valueOf(System.getProperty("imconvert.ignore.exitcode"));

		try {
			ProcessUtils.execute(cmd, null, null, false, !ignoreExitCode);
		} catch (InterruptedException e) {
			throw new IOException("Convert interrupted", e);
		}
	}
}