package eu.openanalytics.phaedra.datacapture.scanner.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem;
import eu.openanalytics.phaedra.datacapture.scanner.Activator;
import eu.openanalytics.phaedra.datacapture.scanner.BaseScannerType;
import eu.openanalytics.phaedra.datacapture.scanner.ScanException;
import eu.openanalytics.phaedra.datacapture.scanner.model.ScanJob;

public class FolderScannerType extends BaseScannerType {
	
	@Override
	public void run(ScanJob scanner, IProgressMonitor monitor) throws ScanException {
		monitor.beginTask("Folder Scanner", IProgressMonitor.UNKNOWN);
		
		monitor.subTask("Reading config");
		final FolderScannerConfig cfg;
		try {
			cfg = parse(scanner.getConfig());
		} catch (IOException e) {
			throw new ScanException("Invalid scanner configuration", e);
		}
		
		final Pattern pattern = Pattern.compile(cfg.pattern);
		final Path startPath = Paths.get(cfg.path);
		
		monitor.subTask("Scanning " + cfg.path);
		FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.equals(startPath)) return FileVisitResult.CONTINUE;
				
				String fileName = dir.toFile().getName();
				Matcher matcher = pattern.matcher(fileName);
				if (matcher.matches()) {
					processItem(dir, cfg);
				}
				
				return FileVisitResult.SKIP_SUBTREE;
			}
		};
		
        try {
			Files.walkFileTree(startPath, visitor);
		} catch (IOException e) {
			throw new ScanException("Failed to scan directory", e);
		}
        
		monitor.done();
	}

	private void processItem(Path folder, FolderScannerConfig cfg) {
		String sourcePath = folder.toFile().getAbsolutePath();
		String experimentName = folder.toFile().getName();
		
		// Create a data capture task.
		DataCaptureTask task = DataCaptureService.getInstance().createTask(sourcePath, cfg.protocolId);
		if (cfg.captureConfig != null) task.setConfigId(cfg.captureConfig);
		task.getParameters().put(DataCaptureTask.PARAM_EXPERIMENT_NAME, experimentName);
		task.getParameters().put(DataCaptureTask.PARAM_ALLOW_AUTO_LINK, cfg.allowAutoLink);
		
		if (DataCaptureService.getInstance().isTaskSourceAlreadyCaptured(task)) {
			// Path is already captured. Abort submission.
			EclipseLog.info("Skipping path (already captured): " + sourcePath, Activator.getDefault());
			return;
		}
		
		// Submit to the data capture service, and log an event.
		EclipseLog.info("Submitting data capture job: " + task.getSource(), Activator.getDefault());
		boolean accepted = DataCaptureService.getInstance().queueTask(task);
		int status = accepted ? 0 : -1;
		String msg = "Data capture task submitted" + (accepted ? "" : " but rejected");
		DataCaptureService.getInstance().fireLogEvent(new DataCaptureLogItem("Scanner", status, task, msg, null));
		
		if (!accepted) {
			EclipseLog.error("Data capture task for scanned folder refused: '" + sourcePath + "'", null, Activator.getDefault());
		}
	}
	
	private FolderScannerConfig parse(String config) throws IOException {
		FolderScannerConfig cfg = new FolderScannerConfig();
		Document doc = XmlUtils.parse(config);
		cfg.path = getConfigValue(doc, "/config/path", null);
		cfg.pattern = getConfigValue(doc, "/config/pattern", null);
		cfg.captureConfig = getConfigValue(doc, "/config/captureConfig", null);
		cfg.protocolId = Long.valueOf(getConfigValue(doc, "/config/protocolId", "0"));
		cfg.allowAutoLink = Boolean.valueOf(getConfigValue(doc, "/config/allowAutoLink", "false"));
		return cfg;
	}
	
	private String getConfigValue(Document doc, String xpath, String defaultValue) {
		return Optional.ofNullable(XmlUtils.findString(xpath, doc)).filter(s -> (s != null && !s.isEmpty())).orElse(defaultValue);
	}
	
	private static class FolderScannerConfig {
		public String path;
		public String pattern;
		public String captureConfig;
		public long protocolId;
		public boolean allowAutoLink;
	}
}