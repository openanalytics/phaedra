package eu.openanalytics.phaedra.datacapture.s3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
import eu.openanalytics.phaedra.datacapture.queue.DataCaptureJobQueue;
import eu.openanalytics.phaedra.datacapture.scanner.BaseScannerType;
import eu.openanalytics.phaedra.datacapture.scanner.ScanException;
import eu.openanalytics.phaedra.datacapture.scanner.model.ScanJob;

public class SQSScannerType extends BaseScannerType {

	private static final Pattern S3_URL_PATTERN = Pattern.compile("s3:\\/\\/([^\\/]+)\\/(.*)");
	private static final Gson GSON = new GsonBuilder().create();
	
	@Override
	public void run(ScanJob scanJob, IProgressMonitor monitor) throws ScanException {
		monitor.beginTask("SQS Scanner", IProgressMonitor.UNKNOWN);

 		if (DataCaptureJobQueue.getQueueSize() >= DataCaptureJobQueue.getWorkerPoolSize()) {
 			monitor.done();
 			return;
 		}
 		
		monitor.subTask("Reading config");
		ScannerConfig cfg = null;
		try {
			cfg = parseConfig(scanJob.getConfig());
		} catch (IOException e) {
			throw new ScanException("Failed to parse scan job configuration", e);
		}
		if (cfg.queues == null || cfg.queues.isEmpty()) throw new ScanException("Invalid scanner configuration: no queue specified");
		
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

		for (String queue: cfg.queues) {
			monitor.subTask(String.format("Polling %s for max %d messages", queue, cfg.maxMsgPerRun));
			ReceiveMessageRequest req = new ReceiveMessageRequest(queue).withMaxNumberOfMessages(cfg.maxMsgPerRun);
			ReceiveMessageResult res = sqs.receiveMessage(req);
			
			int submitCount = 0;
			for (Message msg: res.getMessages()) {
				if (canProcess(msg, cfg)) {
					submitTask(msg, cfg);
					submitCount++;
					sqs.deleteMessage(new DeleteMessageRequest(queue, msg.getReceiptHandle()));
				}
			}
			
			// Proceed to the next queue only if this queue was empty.
			if (submitCount > 0) break;
		}
		
		monitor.done();
	}
	
 	private boolean canProcess(Message msg, ScannerConfig cfg) {
 		MessageBody body = parseMessage(msg);
 		if (body == null || body.url == null) return false;
 		
 		long protocolId = getProtocolId(body, cfg);
 		if (protocolId == 0) return false;
 		
 		if (DataCaptureJobQueue.getQueueSize() >= DataCaptureJobQueue.getWorkerPoolSize()) return false;
 		
 		return true;
	}
	
	private void submitTask(Message msg, ScannerConfig cfg) throws ScanException {
		MessageBody body = parseMessage(msg);
		
		Matcher match = S3_URL_PATTERN.matcher(body.url);
		String s3Bucket = null;
		String s3Path = null;
		if (match.matches()) {
			s3Bucket = match.group(1);
			s3Path = match.group(2);
		} else {
			throw new ScanException("Invalid S3 URL: " + body.url);
		}
		
		DataCaptureTask task = createTask(body, cfg);
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
	
	private MessageBody parseMessage(Message msg) {
		MessageBody body = null;
		
		Matcher m = S3_URL_PATTERN.matcher(msg.getBody());
		if (m.matches()) {
			body = new MessageBody();
			body.url = msg.getBody();
		} else {
			try {
				body = GSON.fromJson(msg.getBody(), MessageBody.class);
			} catch (Exception e) {
				// The body is not valid JSON. Null will be returned.
			}
		}
		
		return body;
	}
	
	private DataCaptureTask createTask(MessageBody msg, ScannerConfig cfg) throws ScanException {
		long protocolId = getProtocolId(msg, cfg);
		if (protocolId == 0) throw new ScanException("Cannot create datacapture task: no target protocol specified");
		DataCaptureTask task = DataCaptureService.getInstance().createTask(msg.url, protocolId);
		
		if (msg.captureConfig != null) task.setConfigId(msg.captureConfig);
		else if (cfg.captureConfig != null) task.setConfigId(cfg.captureConfig);
		
		return task;
	}

	private long getProtocolId(MessageBody msg, ScannerConfig cfg) {
		long protocolId = msg.protocolId;
		if (protocolId == 0) protocolId = cfg.protocolId;
		if (protocolId == 0) {
			for (Entry<String, Long> entry: cfg.keyMappings.entrySet()) {
				if (Pattern.matches(entry.getKey(), msg.url)) {
					protocolId = entry.getValue();
					break;
				}
			}
		}
		return protocolId;
	}
	
	private ScannerConfig parseConfig(String config) throws IOException {
		ScannerConfig cfg = new ScannerConfig();
		Document doc = XmlUtils.parse(config);

		cfg.maxMsgPerRun = Integer.parseInt(getConfigValue(doc, "/config/maxMsgPerRun", "10"));
		cfg.createMissingWellFeatures = Boolean.valueOf(getConfigValue(doc, "/config/createMissingWellFeatures", "false"));
		cfg.createMissingSubWellFeatures = Boolean.valueOf(getConfigValue(doc, "/config/createMissingSubWellFeatures", "false"));
		cfg.captureConfig = getConfigValue(doc, "/config/captureConfig", null);
		cfg.protocolId = Long.valueOf(getConfigValue(doc, "/config/protocolId", "0"));
		cfg.downloadScriptId = getConfigValue(doc, "/config/downloadScriptId", "download.s3.files");
		cfg.s3KeyPattern = getConfigValue(doc, "/config/s3KeyPattern", null);
		
		NodeList queuesTags = XmlUtils.findTags("/config/queues/queue", doc);
		for (int i=0; i < queuesTags.getLength(); i++) {
			Element queueTag = (Element) queuesTags.item(i);
			cfg.queues.add(XmlUtils.getNodeValue(queueTag));
		}
		
		NodeList mappingTags = XmlUtils.findTags("/config/mappings/mapping", doc);
		for (int i=0; i < mappingTags.getLength(); i++) {
			Element mappingTag = (Element) mappingTags.item(i);
			String pattern = XmlUtils.getNodeByName(mappingTag, "pattern").getTextContent();
			Long protocolId = Long.valueOf(XmlUtils.getNodeByName(mappingTag, "protocol").getTextContent());
			cfg.keyMappings.put(pattern, protocolId);
		}
		
		return cfg;
	}
	
	private String getConfigValue(Document doc, String xpath, String defaultValue) {
		return Optional.ofNullable(XmlUtils.findString(xpath, doc)).filter(s -> (s != null && !s.isEmpty())).orElse(defaultValue);
	}
	
	private static class MessageBody {
		public String url;
		public String captureConfig;
		public long protocolId;
	}
	
	private static class ScannerConfig {
		public int maxMsgPerRun;
		
		public boolean createMissingWellFeatures;
		public boolean createMissingSubWellFeatures;
		
		public String downloadScriptId;
		public String s3KeyPattern;

		public String captureConfig;
		public long protocolId;
		
		public List<String> queues = new ArrayList<>();
		public Map<String, Long> keyMappings = new HashMap<>();
	}
}
