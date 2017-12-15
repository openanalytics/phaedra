package eu.openanalytics.phaedra.datacapture.columbus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;

import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask.DataCaptureParameter;
import eu.openanalytics.phaedra.datacapture.columbus.prefs.Prefs;
import eu.openanalytics.phaedra.datacapture.columbus.prefs.Prefs.ColumbusLogin;
import eu.openanalytics.phaedra.datacapture.columbus.ws.ColumbusWSClient;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetMeasurements.Measurement;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetPlates.Plate;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetResults.Result;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetScreens.Screen;
import eu.openanalytics.phaedra.datacapture.columbus.ws.operation.GetUsers.User;
import eu.openanalytics.phaedra.datacapture.scanner.BaseScannerType;
import eu.openanalytics.phaedra.datacapture.scanner.ScanException;
import eu.openanalytics.phaedra.datacapture.scanner.model.ScanJob;

public class ColumbusScannerType extends BaseScannerType {

	@Override
	public void run(ScanJob scanJob, IProgressMonitor monitor) throws ScanException {
		monitor.beginTask("Columbus Scanner", IProgressMonitor.UNKNOWN);
		
		monitor.subTask("Reading config");
		final ColumbusScannerConfig cfg;
		try {
			cfg = parse(scanJob.getConfig());
		} catch (IOException e) {
			throw new ScanException("Invalid scanner configuration", e);
		}
		
		ColumbusLogin[] instances = null;
		if (cfg.instanceIds == null || cfg.instanceIds.isEmpty()) {
			// Either provide one instance by configuration.
			ColumbusLogin instance = new ColumbusLogin();
			instance.host = cfg.host;
			instance.port = cfg.port;
			instance.username = cfg.username;
			instance.password = cfg.password;
			instances = new ColumbusLogin[] { instance };
		} else {
			// Or refer to one or more known instances.
			String[] ids = cfg.instanceIds.split(",");
			instances = new ColumbusLogin[ids.length];
			for (int i = 0; i < ids.length; i++) {
				instances[i] = Prefs.load(ids[i].trim());
			}
		}
		
		for (int i = 0; i < instances.length; i++) {
			monitor.subTask("Scanning Columbus instance " + instances[i].host + ":" + instances[i].port);
			try {
				scanInstance(instances[i], cfg, monitor);
			} catch (IOException e) {
				throw new ScanException("I/O Error scanning instance " + instances[i].host + ":" + instances[i].port, e);
			}
		}
		
		monitor.done();
	}
	
	private void scanInstance(ColumbusLogin instance, ColumbusScannerConfig cfg, IProgressMonitor monitor) throws IOException {
		Pattern screenPattern = cfg.screenPattern == null ? null : Pattern.compile(cfg.screenPattern);
		Pattern platePattern = cfg.platePattern == null ? null : Pattern.compile(cfg.platePattern);
		
		try (ColumbusWSClient client = ColumbusService.getInstance().connect(instance.host, instance.port, instance.username, instance.password)) {
			
			// Navigation: UserGroup > User > Screen
			List<User> users = ColumbusService.getInstance().getUsers(client);
			for (User user: users) {
				GetScreens getScreens = new GetScreens(user.userId);
				client.execute(getScreens);
				for (Screen screen: getScreens.getList()) {
					if (screenPattern == null || screenPattern.matcher(screen.screenName).matches()) {
						Map<Long, Result> newResults = new HashMap<>();
						
						// Drill down into the result level to see if there are results that weren't captured before.
						GetPlates getPlates = new GetPlates(screen.screenId);
						client.execute(getPlates);
						for (Plate plate: getPlates.getList()) {
							if (platePattern == null || platePattern.matcher(plate.plateName).matches()) {
								GetMeasurements getMeas = new GetMeasurements(plate.plateId, screen.screenId);
								client.execute(getMeas);
								for (Measurement meas: getMeas.getList()) {
									// Locate the most recent analysis (if any).
									Result latestResult = ColumbusService.getInstance().getLatestResult(client, meas.measurementId);
									if (latestResult == null) continue;
									// If it hasn't been captured before, submit it.
									String identifier = ColumbusService.getInstance().getUniqueResultId(client, latestResult.resultId);
									boolean isNewResult = cfg.forceDuplicateCapture || !DataCaptureService.getInstance().isReadingAlreadyCaptured(identifier);
									if (isNewResult) newResults.put(meas.measurementId, latestResult);
								}
							}
						}
						
						if (!newResults.isEmpty()) submitTask(screen, newResults, user, cfg, instance);
						else EclipseLog.info("Skipping screen (empty or already captured): " + screen.screenName, Activator.getDefault());
					}
				}
			}
		}
	}

	private ColumbusScannerConfig parse(String config) throws IOException {
		ColumbusScannerConfig cfg = new ColumbusScannerConfig();
		Document doc = XmlUtils.parse(config);
		
		cfg.host = getConfigValue(doc, "/config/host", null);
		cfg.port= Integer.valueOf(getConfigValue(doc, "/config/port", "0"));
		cfg.username = getConfigValue(doc, "/config/username", null);
		cfg.password = getConfigValue(doc, "/config/password", null);
		cfg.instanceIds = getConfigValue(doc, "/config/instanceIds", null);
		
		cfg.screenPattern = getConfigValue(doc, "/config/screenPattern", null);
		cfg.platePattern = getConfigValue(doc, "/config/platePattern", null);
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
	
	private void submitTask(Screen screen, Map<Long, Result> newResults, User user, ColumbusScannerConfig config, ColumbusLogin instance) {
		// Create a data capture task.
		String source = "Columbus/" + user.loginname + "/" + screen.screenName;
		DataCaptureTask task = DataCaptureService.getInstance().createTask(source, config.protocolId);
		if (config.captureConfig != null) task.setConfigId(config.captureConfig);
		task.setUser(user.loginname);
		task.getParameters().put(DataCaptureParameter.TargetExperimentName.name(), screen.screenName.trim());
		task.getParameters().put(DataCaptureParameter.CreateMissingWellFeatures.name(), config.createMissingWellFeatures);
		task.getParameters().put(DataCaptureParameter.CreateMissingSubWellFeatures.name(), config.createMissingSubWellFeatures);
		
		ColumbusService.getInstance().setInstanceConfig(task.getParameters(), instance);
		ColumbusService.getInstance().setResultIds(task.getParameters(), newResults);
		
		// Submit to the data capture service, and log an event.
		EclipseLog.info("Submitting data capture task: " + screen.screenName, Activator.getDefault());
		DataCaptureService.getInstance().queueTask(task, "Columbus Scanner");
	}
	
	private static class ColumbusScannerConfig {
		public String host;
		public int port;
		public String username;
		public String password;
		public String instanceIds;
		
		public String screenPattern;
		public String platePattern;
		public boolean forceDuplicateCapture;
		public boolean createMissingWellFeatures;
		public boolean createMissingSubWellFeatures;
		public String captureConfig;
		public long protocolId;
	}
}
