package eu.openanalytics.phaedra.datacapture.util;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import eu.openanalytics.phaedra.base.email.MailException;
import eu.openanalytics.phaedra.base.email.MailService;
import eu.openanalytics.phaedra.base.util.io.StreamUtils;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.datacapture.Activator;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask;
import eu.openanalytics.phaedra.datacapture.DataCaptureTask.DataCaptureParameter;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem;
import eu.openanalytics.phaedra.datacapture.log.DataCaptureLogItem.LogItemSeverity;
import eu.openanalytics.phaedra.datacapture.log.IDataCaptureLogListener;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;

/**
 * Notification sender for data capture and/or import jobs.
 *
 * The following triggers are supported:
 * <ul>
 * <li><b>Capture error</b>: send an error notification to the protocol subscribers and phaedra support.</li>
 * <li><b>Capture complete</b>: send a notification to phaedra support. Only if auto-link is disabled.</li>
 * <li><b>Data link complete</b>: send a notification to the protocol subscribers and phaedra support.</li>
 * </ul>
 */
public class EmailNotifier implements IDataCaptureLogListener {

	private static final String PHAEDRA_SUPPORT_LIST = "phaedra.support";
	private static final String PROTOCOL_LIST_PREFIX = "subscribers.protocol.";
	private static final String EMAIL_TEMPLATE_PATH = "email-templates/";
	
	@Override
	public void logEvent(DataCaptureLogItem item) {
		if (item.severity == LogItemSeverity.Completed && item.reading == null) notifyCaptureComplete(item);
		if (item.severity == LogItemSeverity.Error) notifyError(item);
	}
	
	private void notifyCaptureComplete(DataCaptureLogItem item) {
		String title = "Phaedra: import complete";
		String body = loadTemplate("capture_complete.html");
		if (body == null) return;
		
		Protocol protocol = null;
		String experimentName  = null;
		Experiment exp = (Experiment) item.task.getParameters().get(DataCaptureParameter.TargetExperiment.name());
		if (exp == null) {
			protocol = (Protocol) item.task.getParameters().get(DataCaptureParameter.TargetProtocol.name());
			experimentName = (String) item.task.getParameters().get(DataCaptureParameter.TargetExperimentName.name());
		} else {
			protocol = exp.getProtocol();
			experimentName = exp.getName();
		}
		
		if (protocol == null) return;
		
		List<String> readings = DataCaptureService.streamableList(DataCaptureService.getInstance().getSavedEvents(item.task.getId()))
			.stream()
			.map(e -> e.getReading())
			.filter(r -> r != null)
			.map(id -> id.substring(0, id.lastIndexOf('(')).trim())
			.sorted()
			.collect(Collectors.toList());

		StringBuilder readingList = new StringBuilder();
		readingList.append("<ul>");
		for (String reading: readings) {
			readingList.append("<li>" + reading + "</li>");
		}
		readingList.append("</ul>");
		body = body.replace("${readingList}", readingList.toString());
		body = body.replace("${readingCount}", String.valueOf(readings.size()));
		body = body.replace("${protocolName}", protocol.getName());
		body = body.replace("${experimentName}", experimentName);
		
		String mailingList = PROTOCOL_LIST_PREFIX + protocol.getId();
		try {
			sendMail(title, body.toString(), mailingList);
		} catch (MailException e) {
			EclipseLog.error("Failed to send data capture notification", e, Activator.getDefault());
		}
	}
	
	private void notifyError(DataCaptureLogItem item) {
		String title = "Phaedra: capture error";
		String body = loadTemplate("capture_error.html");
		
		Protocol p = getTargetProtocol(item.task);
		if (p == null) return;
		
		body = body.replace("${protocolName}", p.getName());
		body = body.replace("${taskSource}", item.task.getSource());
		body = body.replace("${errorMessage}", item.message);
		body = body.replace("${errorCause}", StringUtils.getStackTrace(item.errorCause));
		
		String mailingList = PROTOCOL_LIST_PREFIX + p.getId();
		
		try {
			sendMail(title, body.toString(), mailingList);
		} catch (MailException e) {
			EclipseLog.error("Failed to send data capture notification", e, Activator.getDefault());
		}
	}
	
	private void sendMail(String title, String body, String mailingList) throws MailException {
		String sender = "phaedra-dc-server@" + MailService.getInstance().getMailSuffix();
		
		if (!PHAEDRA_SUPPORT_LIST.equalsIgnoreCase(mailingList)) {
			try {
				// Send a separate copy to phaedra support.
				MailService.getInstance().sendMail(sender, PHAEDRA_SUPPORT_LIST, title, body, true, null);
			} catch (MailException e) {}
		}
		
		// If the mailing list does not exist or is empty, abort the mail send.
		try {
			String[] to = MailService.getInstance().getSubscribers(mailingList);
			if (to == null || to.length == 0) {
				EclipseLog.warn("Cannot send notification: invalid mailing list: " + mailingList, Activator.getDefault());
				return;
			}
		} catch (MailException e) {
			EclipseLog.warn("Cannot send notification: invalid mailing list: " + mailingList, Activator.getDefault());
			return;
		}
		
		MailService.getInstance().sendMail(sender, mailingList, title, body, true, null);
	}

	private String loadTemplate(String name) {
		try {
			URL templateURL = Activator.getDefault().getBundle().getEntry(EMAIL_TEMPLATE_PATH + name);
			return new String(StreamUtils.readAll(templateURL.openStream()));
		} catch (IOException e) {
			EclipseLog.error("Failed to load mail template '" + name + "'", e, Activator.getDefault());
			return null;
		}
	}
	
	private Protocol getTargetProtocol(DataCaptureTask task) {
		Protocol protocol = (Protocol) task.getParameters().get(DataCaptureParameter.TargetProtocol.name());
		if (protocol == null) {
			Experiment experiment = (Experiment) task.getParameters().get(DataCaptureParameter.TargetExperiment.name());
			if (experiment == null) {
				EclipseLog.warn("Cannot find target protocol or experiment for task " + task.getId(), Activator.PLUGIN_ID);
				return null;
			}
			protocol = experiment.getProtocol();
		}
		return protocol;
	}
}
