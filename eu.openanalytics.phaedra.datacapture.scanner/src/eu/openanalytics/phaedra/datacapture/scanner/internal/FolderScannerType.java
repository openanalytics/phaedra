package eu.openanalytics.phaedra.datacapture.scanner.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask.DataCaptureParameter;
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
		
		EclipseLog.debug("Scanning path: " + cfg.path, FolderScannerType.class);
		monitor.subTask("Scanning " + cfg.path);
		
		if (cfg.scanDepth == 0) {
			// Classic scan: root > experiments > plates
			scanClassic(cfg);
		} else {
			// Scan recursively for plates at max depth cfg.scanDepth.
			scanMaxDepth(cfg);
		}
		
		

		monitor.done();
	}

	private void scanClassic(FolderScannerConfig cfg) throws ScanException {
		Pattern expPattern = Pattern.compile(cfg.experimentPattern);
		try (Stream<Path> contents = streamContents(Paths.get(cfg.path), 0)) {
			contents
				.filter(p -> expPattern.matcher(p.getFileName().toString()).matches())
				.filter(p -> Files.isDirectory(p))
				.forEach(p -> processExperimentFolder(p, cfg));
		} catch (Exception e) {
			throw new ScanException("Failed to scan directory", e);
		}
	}
	
	private void processExperimentFolder(Path expFolder, FolderScannerConfig cfg) {
		Pattern platePattern = Pattern.compile(cfg.platePattern);
		
		String[] plateIds = {};
		try (Stream<Path> contents = streamContents(expFolder, 0)) {
			plateIds = contents
					.filter(p -> platePattern.matcher(p.getFileName().toString()).matches())
					.filter(p -> {
						if (!Files.isDirectory(p) || cfg.plateInProgressFlag == null) return true;
						try (Stream<Path> subContents = streamContents(p, 0)) {
							return subContents.noneMatch(child -> child.getFileName().toString().equalsIgnoreCase(cfg.plateInProgressFlag));
						}
					})
					.filter(p -> {
						if (cfg.forceDuplicateCapture) return true;
						return !DataCaptureService.getInstance().isReadingAlreadyCaptured(p.toFile().getAbsolutePath());
					})
					.map(p -> p.toFile().getAbsolutePath())
					.toArray(i -> new String[i]);
		}

		if (plateIds.length == 0) {
			EclipseLog.debug("Skipping folder (already captured): " + expFolder, FolderScannerType.class);
			return;
		}
		
		String sourcePath = expFolder.toFile().getAbsolutePath();
		String experimentName = expFolder.toFile().getName();
		
		// Create a data capture task.
		DataCaptureTask task = DataCaptureService.getInstance().createTask(sourcePath, cfg.protocolId);
		if (cfg.captureConfig != null) task.setConfigId(cfg.captureConfig);
		
		task.getParameters().put(DataCaptureParameter.TargetExperimentName.name(), experimentName);
		task.getParameters().put(DataCaptureParameter.CreateMissingWellFeatures.name(), cfg.createMissingWellFeatures);
		task.getParameters().put(DataCaptureParameter.CreateMissingSubWellFeatures.name(), cfg.createMissingSubWellFeatures);
		task.getParameters().put("plateIds", plateIds);
		
		// Submit to the data capture service, and log an event.
		EclipseLog.info(String.format("Submitting data capture job (%d readings): %s", plateIds.length, task.getSource()), Activator.getDefault());
		DataCaptureService.getInstance().queueTask(task, "Folder Scanner");
	}
	
	private void scanMaxDepth(FolderScannerConfig cfg) throws ScanException {
		Path startPath = Paths.get(cfg.path);
		Pattern platePattern = Pattern.compile(cfg.platePattern);
		try (Stream<Path> contents = streamContents(startPath, cfg.scanDepth)) {
			List<Path> platesToSubmit = Collections.synchronizedList(new ArrayList<>());
			
			contents
				.filter(p -> platePattern.matcher(p.getFileName().toString()).matches())
				.filter(p -> {
					if (!Files.isDirectory(p) || cfg.plateInProgressFlag == null) return true;
					try (Stream<Path> subContents = streamContents(p, 0)) {
						return subContents.noneMatch(child -> child.getFileName().toString().equalsIgnoreCase(cfg.plateInProgressFlag));
					}
				})
				.filter(p -> !platesToSubmit.contains(p.getParent()))
				.filter(p -> {
					if (cfg.forceDuplicateCapture) return true;
					return !DataCaptureService.getInstance().isReadingAlreadyCaptured(p.toFile().getAbsolutePath());
				})
				.forEach(p -> platesToSubmit.add(p));
			
			for (Path p: platesToSubmit) {
				// Create a data capture task.
				DataCaptureTask task = DataCaptureService.getInstance().createTask(p.toFile().getAbsolutePath(), cfg.protocolId);
				if (cfg.captureConfig != null) task.setConfigId(cfg.captureConfig);
				
				task.getParameters().put(DataCaptureParameter.TargetExperimentName.name(), p.getParent().getFileName().toString());
				task.getParameters().put(DataCaptureParameter.CreateMissingWellFeatures.name(), cfg.createMissingWellFeatures);
				task.getParameters().put(DataCaptureParameter.CreateMissingSubWellFeatures.name(), cfg.createMissingSubWellFeatures);
				
				// Submit to the data capture service, and log an event.
				EclipseLog.info(String.format("Submitting data capture job (1 reading): %s", task.getSource()), Activator.getDefault());
				DataCaptureService.getInstance().queueTask(task, "Folder Scanner");
			}
		} catch (Exception e) {
			throw new ScanException("Failed to scan directory", e);
		}
	}
	
	private Stream<Path> streamContents(Path p, int maxDepth) {
		try {
			if (maxDepth == 0) return Files.list(p);
			else return Files.walk(p, maxDepth);
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
		cfg.scanDepth = Integer.valueOf(getConfigValue(doc, "/config/scanDepth", "0"));
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
		public int scanDepth;
		public String plateInProgressFlag;
		public boolean forceDuplicateCapture;
		public boolean createMissingWellFeatures;
		public boolean createMissingSubWellFeatures;
		public String captureConfig;
		public long protocolId;
	}
}