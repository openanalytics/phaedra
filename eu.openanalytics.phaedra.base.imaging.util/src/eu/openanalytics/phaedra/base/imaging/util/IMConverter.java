package eu.openanalytics.phaedra.base.imaging.util;

import java.io.IOException;

import eu.openanalytics.phaedra.base.util.process.ProcessHolder;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;

public class IMConverter extends ProcessHolder {

	private static IMConverter instance;
	
	private final static String[] REQUIRED_FILES = {
		"os/win32/${arch}/convert.exe",
		"os/win32/${arch}/vcomp100.dll"
	};
	
	public void initialize() throws IOException {
		String arch = ProcessUtils.isSystem64Bit() ? "x86_64" : "x86";
		
		String[] requiredFiles = new String[REQUIRED_FILES.length];
		for (int i=0; i<requiredFiles.length; i++) {
			requiredFiles[i] = REQUIRED_FILES[i].replace("${arch}", arch);
		}
		
		super.initialize(Activator.getContext().getBundle().getSymbolicName(), requiredFiles);
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
			if (instance == null) {
				instance = new IMConverter();
				instance.initialize();
			}
		}
		
		String[] cmd = new String[args.length + 1];
		cmd[0] = instance.getDir() + "/convert.exe";
		for (int i=0; i<args.length; i++) cmd[1+i] = args[i];
		
		try {
			ProcessUtils.execute(cmd, null, null, false, true);
		} catch (InterruptedException e) {
			throw new IOException("Convert interrupted", e);
		}
	}
}
