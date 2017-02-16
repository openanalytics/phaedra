package eu.openanalytics.phaedra.datacapture.scanner.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
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
		if (cfg.path == null || cfg.path.isEmpty()) throw new ScanException("Invalid scanner configuration: no path specified");

		monitor.subTask("Scanning " + cfg.path);
		Pattern expPattern = Pattern.compile(cfg.experimentPattern);
		try {
			streamContents(Paths.get(cfg.path))
				.filter(p -> expPattern.matcher(p.getFileName().toString()).matches())
				.filter(p -> Files.isDirectory(p))
				.forEach(p -> processExperimentFolder(p, cfg));
		} catch (Exception e) {
			throw new ScanException("Failed to scan directory", e);
		}

		monitor.done();
	}

	private void processExperimentFolder(Path expFolder, FolderScannerConfig cfg) {
		Pattern platePattern = Pattern.compile(cfg.platePattern);
		
		String[] plateIds = streamContents(expFolder)
			.filter(p -> platePattern.matcher(p.getFileName().toString()).matches())
			.filter(p -> {
				if (!Files.isDirectory(p) || cfg.plateInProgressFlag == null) return true;
				return streamContents(p).noneMatch(child -> child.getFileName().toString().equalsIgnoreCase(cfg.plateInProgressFlag));
			})
			.filter(p -> {
				if (cfg.forceDuplicateCapture) return true;
				return !DataCaptureService.getInstance().isReadingAlreadyCaptured(p.toFile().getAbsolutePath());
			})
			.map(p -> p.toFile().getAbsolutePath())
			.toArray(i -> new String[i]);
		
		if (plateIds.length == 0) {
			EclipseLog.info("Skipping folder (already captured): " + expFolder, Activator.getDefault());
			return;
		}
		
		String sourcePath = expFolder.toFile().getAbsolutePath();
		String experimentName = expFolder.toFile().getName();
		
		// Create a data capture task.
		DataCaptureTask task = DataCaptureService.getInstance().createTask(sourcePath, cfg.protocolId);
		if (cfg.captureConfig != null) task.setConfigId(cfg.captureConfig);
		task.getParameters().put(DataCaptureTask.PARAM_EXPERIMENT_NAME, experimentName);
		task.getParameters().put(DataCaptureTask.PARAM_ALLOW_AUTO_LINK, true);
		task.getParameters().put(DataCaptureTask.PARAM_CREATE_NEW_EXP, false);
		task.getParameters().put(DataCaptureTask.PARAM_CREATE_MISSING_WELL_FEATURES, cfg.createMissingWellFeatures);
		task.getParameters().put(DataCaptureTask.PARAM_CREATE_MISSING_SUBWELL_FEATURES, cfg.createMissingSubWellFeatures);
		task.getParameters().put("plateIds", plateIds);
		
		// Submit to the data capture service, and log an event.
		EclipseLog.info(String.format("Submitting data capture job (%d readings): %s", plateIds.length, task.getSource()), Activator.getDefault());
		DataCaptureService.getInstance().queueTask(task, "Folder Scanner");
	}
	
	private Stream<Path> streamContents(Path p) {
		try {
			return Files.list(p);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private FolderScannerConfig parse(String config) throws IOException {
		FolderScannerConfig cfg = new FolderScannerConfig();
		Document doc = XmlUtils.parse(config);
		cfg.path = getConfigValue(doc, "/config/path", null);
		cfg.experimentPattern = getConfigValue(doc, "/config/experimentPattern", ".*");
		cfg.platePattern = getConfigValue(doc, "/config/platePattern", ".*");
		cfg.plateInProgressFlag = getConfigValue(doc, "/config/plateInProgressFlag", null);
		cfg.forceDuplicateCapture = Boolean.valueOf(getConfigValue(doc, "/config/forceDuplicateCapture", "false"));
		cfg.createMissingWellFeatures = Boolean.valueOf(getConfigValue(doc, "/config/createMissingWellFeatures", "false"));
		cfg.createMissingSubWellFeatures = Boolean.valueOf(getConfigValue(doc, "/config/createMissingSubWellFeatures", "false"));
		cfg.captureConfig = getConfigValue(doc, "/config/captureConfig", null);
		cfg.protocolId = Long.valueOf(getConfigValue(doc, "/config/protocolId", "0"));
		return cfg;
	}
	
	private String getConfigValue(Document doc, String xpath, String defaultValue) {
		return Optional.ofNullable(XmlUtils.findString(xpath, doc)).filter(s -> (s != null && !s.isEmpty())).orElse(defaultValue);
	}
	
	private static class FolderScannerConfig {
		public String path;
		public String experimentPattern;
		public String platePattern;
		public String plateInProgressFlag;
		public boolean forceDuplicateCapture;
		public boolean createMissingWellFeatures;
		public boolean createMissingSubWellFeatures;
		public String captureConfig;
		public long protocolId;
	}
}