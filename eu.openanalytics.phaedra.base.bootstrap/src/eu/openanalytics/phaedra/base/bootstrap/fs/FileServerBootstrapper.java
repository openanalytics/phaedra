package eu.openanalytics.phaedra.base.bootstrap.fs;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.core.runtime.FileLocator;

import eu.openanalytics.phaedra.base.bootstrap.Activator;
import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.bootstrap.BootstrapException;
import eu.openanalytics.phaedra.base.environment.bootstrap.IBootstrapper;

public class FileServerBootstrapper implements IBootstrapper {

	@Override
	public void bootstrap(IEnvironment env) throws BootstrapException {
		try {
			URL fsPath = FileLocator.find(Activator.getDefault().getBundle(), new org.eclipse.core.runtime.Path("/fs"), null);
			fsPath = FileLocator.toFileURL(fsPath);
			final Path basePath = Paths.get(fsPath.toURI());
			Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path relativePath = basePath.relativize(file);
					env.getFileServer().putContents(relativePath.toString(), file.toFile());
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException | URISyntaxException e) {
			throw new BootstrapException("Failed to copy file server contents", e);
		}
	}

}
