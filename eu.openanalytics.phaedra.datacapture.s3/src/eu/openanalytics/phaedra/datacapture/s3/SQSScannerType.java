package eu.openanalytics.phaedra.datacapture.s3;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import eu.openanalytics.phaedra.base.environment.IEnvironment;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.xml.XmlUtils;
import eu.openanalytics.phaedra.datacapture.DataCaptureException;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask.DataCaptureParameter;
import eu.openanalytics.phaedra.datacapture.config.ModuleConfig;
import eu.openanalytics.phaedra.datacapture.config.ParameterGroup;
import eu.openanalytics.phaedra.datacapture.module.IModule;
import eu.openanalytics.phaedra.datacapture.module.ModuleFactory;
import eu.openanalytics.phaedra.datacapture.scanner.BaseScannerType;
import eu.openanalytics.phaedra.datacapture.scanner.ScanException;
import eu.openanalytics.phaedra.datacapture.scanner.model.ScanJob;

public class SQSScannerType extends BaseScannerType {

	private static final Pattern s3PathPattern = Pattern.compile("s3:\\/\\/([^\\/]+)\\/(.*)");
	
	@Override
	public void run(ScanJob scanJob, IProgressMonitor monitor) throws ScanException {
		monitor.beginTask("SQS Scanner", IProgressMonitor.UNKNOWN);

		monitor.subTask("Reading config");
		ScannerConfig cfg = null;
		try {
			cfg = parse(scanJob.getConfig());
		} catch (IOException e) {
			throw new ScanException("Failed to parse scan job configuration", e);
		}
		if (cfg.queue == null || cfg.queue.isEmpty()) throw new ScanException("Invalid scanner configuration: no queue specified");
		
		monitor.subTask(String.format("Polling %s for max %d messages", cfg.queue, cfg.maxMsgPerRun));
		
		BasicAWSCredentials credentials = null;
		try {
			IEnvironment env = Screening.getEnvironment();
			String key = env.getConfig().getValue(env.getName(), "fs", "user");
			String secret = env.getConfig().resolvePassword(env.getName(), "fs");
			credentials = new BasicAWSCredentials(key, secret);
		} catch (IOException e) {
			throw new ScanException("Failed to load API key credentials", e);
		}
		AmazonSQS sqs = AmazonSQSClientBuilder
				.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();

		ReceiveMessageRequest req = new ReceiveMessageRequest(cfg.queue).withMaxNumberOfMessages(cfg.maxMsgPerRun);
		ReceiveMessageResult res = sqs.receiveMessage(req);
		for (Message msg: res.getMessages()) {
			if (canProcess(msg, cfg)) {
				submitTask(msg, cfg);
				sqs.deleteMessage(new DeleteMessageRequest(cfg.queue, msg.getReceiptHandle()));
			}
		}
		
		monitor.done();
	}
	
	private boolean canProcess(Message msg, ScannerConfig cfg) {
		//TODO Perform additional checks on the message contents
		return s3PathPattern.matcher(msg.getBody()).matches();
	}
	
	private void submitTask(Message msg, ScannerConfig cfg) throws ScanException {
		//TODO Allow s3 path to be specified in attributes or structured body
		String sourcePath = msg.getBody();
		Matcher match = s3PathPattern.matcher(sourcePath);
		String s3Bucket = null;
		String s3Path = null;
		if (match.matches()) {
			s3Bucket = match.group(1);
			s3Path = match.group(2);
		} else {
			throw new ScanException("Invalid message body: " + sourcePath);
		}
		
		DataCaptureTask task = DataCaptureService.getInstance().createTask(sourcePath, cfg.protocolId);
		if (cfg.captureConfig != null) task.setConfigId(cfg.captureConfig);
		
		try {
			ModuleConfig modCfg = new ModuleConfig();
			modCfg.setType("ScriptedModule");
			modCfg.setParameters(new ParameterGroup());
			modCfg.getParameters().setParameter("script.id", cfg.downloadScriptId);
			modCfg.getParameters().setParameter("s3.bucket", s3Bucket);
			modCfg.getParameters().setParameter("s3.path", s3Path);
			if (cfg.s3KeyPattern != null) modCfg.getParameters().setParameter("s3.key.pattern", cfg.s3KeyPattern);
			
			IModule module = ModuleFactory.createModule(modCfg);
			module.configure(modCfg);

			modCfg.getParameters().setParameter("output.path.variable", "s3.files.downloaded");
			task.getParameters().put("source.path", "${s3.files.downloaded}");
			
			task.getParameters().put(DataCaptureParameter.PreModules.name(), new IModule[] { module });
			task.getParameters().put(DataCaptureParameter.CreateMissingWellFeatures.name(), cfg.createMissingWellFeatures);
			task.getParameters().put(DataCaptureParameter.CreateMissingSubWellFeatures.name(), cfg.createMissingSubWellFeatures);
			
			EclipseLog.info(String.format("Submitting data capture job: %s", task.getSource()), Activator.PLUGIN_ID);
			DataCaptureService.getInstance().queueTask(task, "SQS Scanner");
		} catch (DataCaptureException e) {
			throw new ScanException("Failed to prepare datacapture task", e);
		}
	}

	private ScannerConfig parse(String config) throws IOException {
		ScannerConfig cfg = new ScannerConfig();
		Document doc = XmlUtils.parse(config);
		cfg.queue = getConfigValue(doc, "/config/queue", null);
		cfg.maxMsgPerRun = Integer.parseInt(getConfigValue(doc, "/config/maxMsgPerRun", "10"));
		cfg.createMissingWellFeatures = Boolean.valueOf(getConfigValue(doc, "/config/createMissingWellFeatures", "false"));
		cfg.createMissingSubWellFeatures = Boolean.valueOf(getConfigValue(doc, "/config/createMissingSubWellFeatures", "false"));
		cfg.captureConfig = getConfigValue(doc, "/config/captureConfig", null);
		cfg.protocolId = Long.valueOf(getConfigValue(doc, "/config/protocolId", "0"));
		cfg.s3KeyPattern = getConfigValue(doc, "/config/s3KeyPattern", null);
		cfg.downloadScriptId = getConfigValue(doc, "/config/downloadScriptId", "download.s3.files");
		return cfg;
	}
	
	private String getConfigValue(Document doc, String xpath, String defaultValue) {
		return Optional.ofNullable(XmlUtils.findString(xpath, doc)).filter(s -> (s != null && !s.isEmpty())).orElse(defaultValue);
	}
	
	private static class ScannerConfig {
		public String queue;
		public int maxMsgPerRun;
		
		public boolean createMissingWellFeatures;
		public boolean createMissingSubWellFeatures;
		
		public String captureConfig;
		public long protocolId;
		public String s3KeyPattern;
		public String downloadScriptId;
	}
}
