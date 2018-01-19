package eu.openanalytics.phaedra.base.fs;

import eu.openanalytics.phaedra.base.util.misc.ConfigResolver;

public class FileServerConfig extends ConfigResolver {

	public static final String PATH = "path";
	public static final String USERNAME = "user";
	public static final String PASSWORD = "password";
	
	public FileServerConfig() {
		super("phaedra.fs.");
	}
}
