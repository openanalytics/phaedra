package eu.openanalytics.phaedra.base.fs.store;

import java.io.IOException;
import java.nio.file.AccessMode;

import eu.openanalytics.phaedra.base.fs.SecureFileServer;

public class FileStoreFactory {

	public static IFileStore open(String fsPath, AccessMode mode, SecureFileServer fs) throws IOException {
		return new ZippedFileStore(fsPath, mode, fs);
	}

}
